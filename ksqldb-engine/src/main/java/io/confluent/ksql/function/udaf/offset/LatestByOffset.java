/*
 * Copyright 2019 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"; you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.function.udaf.offset;

import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.SEQ_FIELD;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.STRUCT_BOOLEAN;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.STRUCT_DOUBLE;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.STRUCT_INTEGER;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.STRUCT_LONG;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.STRUCT_STRING;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.VAL_FIELD;
import static io.confluent.ksql.function.udaf.KudafByOffsetUtils.compareStructs;

import io.confluent.ksql.function.udaf.Udaf;
import io.confluent.ksql.function.udaf.UdafDescription;
import io.confluent.ksql.function.udaf.UdafFactory;
import io.confluent.ksql.util.KsqlConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

@UdafDescription(
    name = "LATEST_BY_OFFSET",
    description = LatestByOffset.DESCRIPTION,
    author = KsqlConstants.CONFLUENT_AUTHOR
)
public final class LatestByOffset {

  static final String DESCRIPTION =
      "This function returns the most recent n values for the column, computed by offset.";

  private LatestByOffset() {
  }

  static AtomicLong sequence = new AtomicLong();
  
  static final Comparator<Struct> structComparator  = new LatestStructComparator();

  @UdafFactory(description = "return the latest value of an integer column",
      paramSchema = "STRUCT<SEQ BIGINT, VAL INT>",
      aggregateSchema = "STRUCT<SEQ BIGINT, VAL INT>")
  public static Udaf<Integer, Struct, Integer> latestInteger() {
    return latest(STRUCT_INTEGER);
  }
  
  @UdafFactory(description = "return the latest value of an integer column",
      paramSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL INT>>",
      aggregateSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL INT>>")
  public static Udaf<Integer, List<Struct>, List<Integer>> latestInteger(final int topNSize) {
    return latestN(STRUCT_INTEGER, topNSize);
  }

  @UdafFactory(description = "return the latest value of an big integer column",
      paramSchema = "STRUCT<SEQ BIGINT, VAL BIGINT>",
      aggregateSchema = "STRUCT<SEQ BIGINT, VAL BIGINT>")
  public static Udaf<Long, Struct, Long> latestLong() {
    return latest(STRUCT_LONG);
  }
  
  @UdafFactory(description = "return the latest value of an big integer column",
      paramSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL BIGINT>>",
      aggregateSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL BIGINT>>")
  public static Udaf<Long, List<Struct>, List<Long>> latestLong(final int topNSize) {
    return latestN(STRUCT_LONG, topNSize);
  }

  @UdafFactory(description = "return the latest value of a double column",
      paramSchema = "STRUCT<SEQ BIGINT, VAL DOUBLE>",
      aggregateSchema = "STRUCT<SEQ BIGINT, VAL DOUBLE>")
  public static Udaf<Double, Struct, Double> latestDouble() {
    return latest(STRUCT_DOUBLE);
  }
  
  @UdafFactory(description = "return the latest value of a double column",
      paramSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL DOUBLE>>",
      aggregateSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL DOUBLE>>")
  public static Udaf<Double, List<Struct>, List<Double>> latestDouble(final int topNSize) {
    return latestN(STRUCT_DOUBLE, topNSize);
  }

  @UdafFactory(description = "return the latest value of a boolean column",
      paramSchema = "STRUCT<SEQ BIGINT, VAL BOOLEAN>",
      aggregateSchema = "STRUCT<SEQ BIGINT, VAL BOOLEAN>")
  public static Udaf<Boolean, Struct, Boolean> latestBoolean() {
    return latest(STRUCT_BOOLEAN);
  }
  
  @UdafFactory(description = "return the latest value of a boolean column",
      paramSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL BOOLEAN>>",
      aggregateSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL BOOLEAN>>")
  public static Udaf<Boolean, List<Struct>, List<Boolean>> latestBoolean(final int topNSize) {
    return latestN(STRUCT_BOOLEAN, topNSize);
  }

  @UdafFactory(description = "return the latest value of a string column",
      paramSchema = "STRUCT<SEQ BIGINT, VAL STRING>",
      aggregateSchema = "STRUCT<SEQ BIGINT, VAL STRING>")
  public static Udaf<String, Struct, String> latestString() {
    return latest(STRUCT_STRING);
  }
  
  @UdafFactory(description = "return the latest value of a string column",
      paramSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL STRING>>",
      aggregateSchema = "ARRAY<STRUCT<SEQ BIGINT, VAL STRING>>")
  public static Udaf<String, List<Struct>, List<String>> latestString(final int topNSize) {
    return latestN(STRUCT_STRING, topNSize);
  }
  
  static <T> Struct createStruct(final Schema schema, final T val) {
    final Struct struct = new Struct(schema);
    struct.put(SEQ_FIELD, generateSequence());
    struct.put(VAL_FIELD, val);
    return struct;
  }

  private static long generateSequence() {
    return sequence.getAndIncrement();
  }

  @UdafFactory(description = "Latest by offset")
  static <T> Udaf<T, Struct, T> latest(final Schema structSchema) {
    return new Udaf<T, Struct, T>() {

      @Override
      public Struct initialize() {
        return createStruct(structSchema, null);
      }

      @Override
      public Struct aggregate(final T current, final Struct aggregate) {
        if (current == null) {
          return aggregate;
        } else {
          return createStruct(structSchema, current);
        }
      }

      @Override
      public Struct merge(final Struct aggOne, final Struct aggTwo) {
        // When merging we need some way of evaluating the "latest' one.
        // We do this by keeping track of the sequence of when it was originally processed
        if (compareStructs(aggOne, aggTwo) >= 0) {
          return aggOne;
        } else {
          return aggTwo;
        }
      }

      @Override
      @SuppressWarnings("unchecked")
      public T map(final Struct agg) {
        return (T) agg.get(VAL_FIELD);
      }
    };
  }
  
  @UdafFactory(description = "Latest N by offset")
  static <T> Udaf<T, List<Struct>, List<T>> latestN(final Schema structSchema, final int topNSize) {
    return new Udaf<T, List<Struct>, List<T>>() {
      @Override
      public List<Struct> initialize() {
        return new ArrayList<Struct>();
      }

      @Override
      public List<Struct> aggregate(final T current, final List<Struct> aggregate) {
        if (current == null) {
          return aggregate;
        }

        aggregate.add(createStruct(structSchema, current));
        final int currentSize = aggregate.size();
        if (currentSize <= topNSize) {
          return aggregate;
        } else {
          return aggregate.subList(currentSize - topNSize, currentSize);
        }
      }

      @Override
      public List<Struct> merge(final List<Struct> aggOne, final List<Struct> aggTwo) {
        final List<Struct> merged = new ArrayList<>();
        merged.addAll(aggOne);
        merged.addAll(aggTwo);
        final int currentSize = merged.size();
        Collections.sort(merged, structComparator);
        if (currentSize <= topNSize) {
          return merged;
        } else {
          return merged.subList(currentSize - topNSize, currentSize);
        }
      }

      @Override
      @SuppressWarnings("unchecked")
      public List<T> map(final List<Struct> agg) {
        return (List<T>) agg.stream().map(s -> s.get(VAL_FIELD)).collect(Collectors.toList());
      }
    };
  }

}
