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

package io.confluent.ksql.test.parser;

import io.confluent.ksql.parser.KsqlParser.ParsedStatement;
import io.confluent.ksql.parser.tree.AssertStatement;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A single line of a test execution. A {@code TestStatement} can consist of
 * either:
 * <ul>
 *   <li> a {@link ParsedStatement}, which will issue an update to the ksqlDB, or </li>
 *   <li> an {@link AssertStatement}, which will ensure data is accurate, or </li>
 *   <li> a {@link TestDirective}, which will ensure some metadata on the test </li>
 * </ul>
 */
public final class TestStatement {

  private final ParsedStatement engineStatement;
  private final AssertStatement assertStatement;
  private final TestDirective directive;

  public static TestStatement of(final ParsedStatement engineStatement) {
    return new TestStatement(engineStatement, null, null);
  }

  public static TestStatement of(final AssertStatement assertStatement) {
    return new TestStatement(null, assertStatement, null);
  }

  public static TestStatement of(final TestDirective directive) {
    return new TestStatement(null, null, directive);
  }

  private TestStatement(
      final ParsedStatement engineStatement,
      final AssertStatement assertStatement,
      final TestDirective directive
  ) {
    this.engineStatement = engineStatement;
    this.assertStatement = assertStatement;
    this.directive = directive;

    final boolean exactlyOne = Stream.of(engineStatement, assertStatement, directive)
        .filter(Objects::nonNull)
        .count() == 1;

    if (!exactlyOne) {
      throw new IllegalStateException(String.format(
          "Expected exactly one of engine, assert or directive statement. Got (%s, %s, %s).",
          engineStatement,
          assertStatement,
          directive));
    }
  }

  public void handle(
      final Consumer<ParsedStatement> parsedStatementConsumer,
      final Consumer<AssertStatement> assertStatementConsumer,
      final Consumer<TestDirective> testDirectiveConsumer
  ) {
    if (engineStatement != null) {
      parsedStatementConsumer.accept(engineStatement);
    } else if (assertStatement != null) {
      assertStatementConsumer.accept(assertStatement);
    } else if (directive != null) {
      testDirectiveConsumer.accept(directive);
    }
  }

  public boolean hasEngineStatement() {
    return engineStatement != null;
  }

  public ParsedStatement getEngineStatement() {
    if (!hasEngineStatement()) {
      throw new NoSuchElementException("engineStatement");
    }

    return engineStatement;
  }

  public boolean hasAssertStatement() {
    return assertStatement != null;
  }

  public AssertStatement getAssertStatement() {
    if (!hasAssertStatement()) {
      throw new NoSuchElementException("assertStatement");
    }

    return assertStatement;
  }

  public boolean hasDirective() {
    return directive != null;
  }

  public TestDirective getDirective() {
    if (!hasDirective()) {
      throw new NoSuchElementException("directive");
    }

    return directive;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TestStatement that = (TestStatement) o;
    return Objects.equals(engineStatement, that.engineStatement)
        && Objects.equals(assertStatement, that.assertStatement)
        && Objects.equals(directive, that.directive);
  }

  @Override
  public String toString() {
    return "TestStatement{"
        + "engineStatement=" + engineStatement
        + ", assertStatement=" + assertStatement
        + ", directive=" + directive
        + '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(engineStatement, assertStatement, directive);
  }
}
