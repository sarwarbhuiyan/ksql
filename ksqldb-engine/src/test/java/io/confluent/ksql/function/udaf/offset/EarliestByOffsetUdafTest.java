/*
 * Copyright 2020 Confluent Inc.
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import io.confluent.ksql.function.udaf.Udaf;
import org.apache.kafka.connect.data.Struct;
import org.junit.Test;

public class EarliestByOffsetUdafTest {
  @Test
  public void shouldInitialize() {
    // Given:
    final Udaf<Integer, Struct, Integer> udaf = EarliestByOffset
        .earliest(STRUCT_LONG);

    // When:
    Struct init = udaf.initialize();

    // Then:
    assertThat(init, is(notNullValue()));
  }
  
  @Test
  public void shouldInitializeN() {
    // Given:
    final Udaf<Integer, List<Struct>, List<Integer>> udaf = EarliestByOffset
        .earliestN(STRUCT_LONG, 2);

    // When:
    List<Struct> init = udaf.initialize();

    // Then:
    assertThat(init, is(notNullValue()));
  }

  @Test
  public void shouldComputeEarliestInteger() {
    // Given:
    final Udaf<Integer, Struct, Integer> udaf = EarliestByOffset.earliestInteger();

    // When:
    Struct res = udaf
        .aggregate(123, EarliestByOffset.createStruct(STRUCT_INTEGER, 321));

    // Then:
    assertThat(res.get(VAL_FIELD), is(321));
  }
  
  @Test
  public void shouldComputeEarliest2Integers() {
    // Given:
    final Udaf<Integer, List<Struct>, List<Integer>> udaf = EarliestByOffset.earliestInteger(2);

    // When:
    List<Struct> res = udaf
        .aggregate(123, new ArrayList<>(Arrays.asList(EarliestByOffset.createStruct(STRUCT_INTEGER, 321))));

    // Then:
    assertThat(res.get(0).get(VAL_FIELD), is(321));
    assertThat(res.get(1).get(VAL_FIELD), is(123));
    
    List<Struct> res2 = udaf
        .aggregate(543, res);
    assertThat(res2.size(), is(2));
    assertThat(res2.get(0).get(VAL_FIELD), is(321));
    assertThat(res2.get(1).get(VAL_FIELD), is(123));
    
    
    
  }

  @Test
  public void shouldMerge() {
    // Given:
    final Udaf<Integer, Struct, Integer> udaf = EarliestByOffset.earliestInteger();

    Struct agg1 = EarliestByOffset.createStruct(STRUCT_INTEGER, 123);
    Struct agg2 = EarliestByOffset.createStruct(STRUCT_INTEGER, 321);

    // When:
    Struct merged1 = udaf.merge(agg1, agg2);
    Struct merged2 = udaf.merge(agg2, agg1);

    // Then:
    assertThat(merged1, is(agg1));
    assertThat(merged2, is(agg1));
  }
  
  @Test
  public void shouldMerge2Integers() {
    // Given:
    final Udaf<Integer, List<Struct>, List<Integer>> udaf = EarliestByOffset.earliestInteger(2);

    Struct struct1 = EarliestByOffset.createStruct(STRUCT_INTEGER, 123);
    Struct struct2 = EarliestByOffset.createStruct(STRUCT_INTEGER, 321);
    List<Struct> agg1 = new ArrayList<>(Arrays.asList(struct1, struct2));
    
    Struct struct3 = EarliestByOffset.createStruct(STRUCT_INTEGER, 543);
    Struct struct4 = EarliestByOffset.createStruct(STRUCT_INTEGER, 654);
    List<Struct> agg2 = new ArrayList<>(Arrays.asList(struct3, struct4));
    
    // When:
    List<Struct> merged1 = udaf.merge(agg1, agg2);
    List<Struct> merged2 = udaf.merge(agg2, agg1);

    // Then:
    assertThat(merged1, is(agg1));
    assertThat(merged2, is(agg1));
  }

  @Test
  public void shouldMergeWithOverflow() {
    // Given:
    final Udaf<Integer, Struct, Integer> udaf = EarliestByOffset.earliestInteger();

    EarliestByOffset.sequence.set(Long.MAX_VALUE);

    Struct agg1 = EarliestByOffset.createStruct(STRUCT_INTEGER, 123);
    Struct agg2 = EarliestByOffset.createStruct(STRUCT_INTEGER, 321);

    // When:
    Struct merged1 = udaf.merge(agg1, agg2);
    Struct merged2 = udaf.merge(agg2, agg1);

    // Then:
    assertThat(agg1.getInt64(SEQ_FIELD), is(Long.MAX_VALUE));
    assertThat(agg2.getInt64(SEQ_FIELD), is(Long.MIN_VALUE));
    assertThat(merged1, is(agg1));
    assertThat(merged2, is(agg1));
  }
  
  @Test
  public void shouldMergeWithOverflow2Integers() {
    // Given:
    final Udaf<Integer, List<Struct>, List<Integer>> udaf = EarliestByOffset.earliestInteger(2);

    EarliestByOffset.sequence.set(Long.MAX_VALUE-1);

    Struct struct1 = EarliestByOffset.createStruct(STRUCT_INTEGER, 123);
    Struct struct2 = EarliestByOffset.createStruct(STRUCT_INTEGER, 321);
    List<Struct> agg1 = new ArrayList<>(Arrays.asList(struct1, struct2));
    
    Struct struct3 = EarliestByOffset.createStruct(STRUCT_INTEGER, 543);
    Struct struct4 = EarliestByOffset.createStruct(STRUCT_INTEGER, 654);
    List<Struct> agg2 = new ArrayList<>(Arrays.asList(struct3, struct4));
    

    // When:
    List<Struct> merged1 = udaf.merge(agg1, agg2);
    List<Struct> merged2 = udaf.merge(agg2, agg1);

    // Then:
    assertThat(agg1.get(0).getInt64(SEQ_FIELD), is(Long.MAX_VALUE-1));
    assertThat(agg2.get(0).getInt64(SEQ_FIELD), is(Long.MIN_VALUE));
    assertThat(merged1, is(agg1));
    assertThat(merged2, is(agg1));
  }

  @Test
  public void shouldComputeEarliestLong() {
    // Given:
    final Udaf<Long, Struct, Long> udaf = EarliestByOffset.earliestLong();

    // When:
    Struct res = udaf
        .aggregate(123L, EarliestByOffset.createStruct(STRUCT_LONG, 321L));

    // Then:
    assertThat(res.getInt64(VAL_FIELD), is(321L));
  }
  
  @Test
  public void shouldComputeEarliest2Longs() {
    // Given:
    final Udaf<Long, List<Struct>, List<Long>> udaf = EarliestByOffset.earliestLong(2);

    // When:
    List<Struct> res = udaf
        .aggregate(123L, new ArrayList<>(Arrays.asList(EarliestByOffset.createStruct(STRUCT_LONG, 321L))));

    // Then:
    assertThat(res.get(0).get(VAL_FIELD), is(321L));
    assertThat(res.get(1).get(VAL_FIELD), is(123L));
    
    List<Struct> res2 = udaf
        .aggregate(543L, res);
    assertThat(res2.size(), is(2));
    assertThat(res2.get(0).get(VAL_FIELD), is(321L));
    assertThat(res2.get(1).get(VAL_FIELD), is(123L));
  }

  @Test
  public void shouldComputeEarliestDouble() {
    // Given:
    final Udaf<Double, Struct, Double> udaf = EarliestByOffset.earliestDouble();

    // When:
    Struct res = udaf
        .aggregate(1.1d, EarliestByOffset.createStruct(STRUCT_DOUBLE, 2.2d));

    // Then:
    assertThat(res.getFloat64(VAL_FIELD), is(2.2d));
  }
  
  @Test
  public void shouldComputeEarliest2Doubles() {
    // Given:
    final Udaf<Double, List<Struct>, List<Double>> udaf = EarliestByOffset.earliestDouble(2);

    // When:
    List<Struct> res = udaf
        .aggregate(1.1d, new ArrayList<>(Arrays.asList(EarliestByOffset.createStruct(STRUCT_DOUBLE, 2.2d))));

    // Then:
    assertThat(res.get(0).get(VAL_FIELD), is(2.2d));
    assertThat(res.get(1).get(VAL_FIELD), is(1.1d));
    
    List<Struct> res2 = udaf
        .aggregate(3.3d, res);
    assertThat(res2.size(), is(2));
    assertThat(res2.get(0).get(VAL_FIELD), is(2.2d));
    assertThat(res2.get(1).get(VAL_FIELD), is(1.1d));
  }

  @Test
  public void shouldComputeEarliestBoolean() {
    // Given:
    final Udaf<Boolean, Struct, Boolean> udaf = EarliestByOffset.earliestBoolean();

    // When:
    Struct res = udaf
        .aggregate(true, EarliestByOffset.createStruct(STRUCT_BOOLEAN, false));

    // Then:
    assertThat(res.getBoolean(VAL_FIELD), is(false));
  }
  
  @Test
  public void shouldComputeEarliest2Booleans() {
    // Given:
    final Udaf<Boolean, List<Struct>, List<Boolean>> udaf = EarliestByOffset.earliestBoolean(2);

    // When:
    List<Struct> res = udaf
        .aggregate(true, new ArrayList<>(Arrays.asList(EarliestByOffset.createStruct(STRUCT_BOOLEAN, false))));

    // Then:
    assertThat(res.get(0).get(VAL_FIELD), is(false));
    assertThat(res.get(1).get(VAL_FIELD), is(true));
    
    List<Struct> res2 = udaf
        .aggregate(false, res);
    assertThat(res2.size(), is(2));
    assertThat(res2.get(0).get(VAL_FIELD), is(false));
    assertThat(res2.get(1).get(VAL_FIELD), is(true));
  }

  @Test
  public void shouldComputeEarliestString() {
    // Given:
    final Udaf<String, Struct, String> udaf = EarliestByOffset.earliestString();

    // When:
    Struct res = udaf
        .aggregate("foo", EarliestByOffset.createStruct(STRUCT_STRING, "bar"));

    // Then:
    assertThat(res.getString(VAL_FIELD), is("bar"));
  }
  
  @Test
  public void shouldComputeEarliest2Strings() {
    // Given:
    final Udaf<String, List<Struct>, List<String>> udaf = EarliestByOffset.earliestString(2);

    // When:
    List<Struct> res = udaf
        .aggregate("foo", new ArrayList<>(Arrays.asList(EarliestByOffset.createStruct(STRUCT_STRING, "bar"))));

    // Then:
    assertThat(res.get(0).get(VAL_FIELD), is("bar"));
    assertThat(res.get(1).get(VAL_FIELD), is("foo"));
    
    List<Struct> res2 = udaf
        .aggregate("baz", res);
    assertThat(res2.size(), is(2));
    assertThat(res2.get(0).get(VAL_FIELD), is("bar"));
    assertThat(res2.get(1).get(VAL_FIELD), is("foo"));

  }
}
