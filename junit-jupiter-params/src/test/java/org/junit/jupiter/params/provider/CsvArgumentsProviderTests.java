/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * @since 5.0
 */
class CsvArgumentsProviderTests {

	@Test
	void providesSingleArgument() {
		Stream<Object[]> arguments = provideArguments(',', "", "foo");

		assertThat(arguments).containsExactly(new String[] { "foo" });
	}

	@Test
	void providesMultipleArguments() {
		Stream<Object[]> arguments = provideArguments(',', "", "foo", "bar");

		assertThat(arguments).containsExactly(new String[] { "foo" }, new String[] { "bar" });
	}

	@Test
	void splitsAndTrimsArguments() {
		Stream<Object[]> arguments = provideArguments('|', "", " foo | bar ");

		assertThat(arguments).containsExactly(new String[] { "foo", "bar" });
	}

	@Test
	void understandsQuotes() {
		Stream<Object[]> arguments = provideArguments(',', "", "'foo, bar'");

		assertThat(arguments).containsExactly(new String[] { "foo, bar" });
	}

	@Test
	void understandsEscapeCharacters() {
		Stream<Object[]> arguments = provideArguments(',', "", "'foo or ''bar''', baz");

		assertThat(arguments).containsExactly(new String[] { "foo or 'bar'", "baz" });
	}

	@Test
	void throwsExceptionOnInvalidCsv() {
		JUnitException exception = assertThrows(JUnitException.class,
			() -> provideArguments(',', "", "foo", "bar", "").toArray());

		assertThat(exception).hasMessage("Line at index 2 contains invalid CSV: \"\"");
	}

	@Test
	void emptyValueIsAnEmptyString() {
		Stream<Object[]> arguments = provideArguments(',', "", "null , , empty , ''");

		assertThat(arguments).containsExactly(new String[] { "null", null, "empty", "" });
	}

	@Test
	void emptyValueIsAnEmptyWithCustomEmptyValueString() {
		Stream<Object[]> arguments = provideArguments(',', "vacio", "null , , empty , ''");

		assertThat(arguments).containsExactly(new String[] { "null", null, "empty", "vacio" });
	}

	@Test
	void leadingSpacesAreTrimmed() {
		Stream<Object[]> arguments = provideArguments(',', "", "'', 1", " '', 2", "'' , 3", " '' , 4");

		assertThat(arguments).containsExactly(new Object[][] { { "", "1" }, { "", "2" }, { "", "3" }, { "", "4" } });
	}

	@Test
	void trailingSpacesAreTrimmed() {
		Stream<Object[]> arguments = provideArguments(',', "", "1,''", "2, ''", "3,'' ", "4, '' ");

		assertThat(arguments).containsExactly(new Object[][] { { "1", "" }, { "2", "" }, { "3", "" }, { "4", "" } });
	}

	@Test
	void convertsEmptyValuesToNullInLinesAfterFirst() {
		Stream<Object[]> arguments = provideArguments(',', "", "'', ''", " , ");

		assertThat(arguments).containsExactly(new Object[][] { { "", "" }, { null, null } });
	}

	@Test
	void providesArgumentsWithStringDelimiter() {
		Stream<Object[]> arguments = provideArguments(",", "", "foo, bar", "bar, foo");

		assertThat(arguments).containsExactly(new String[] { "foo", "bar" }, new String[] { "bar", "foo" });
	}

	@Test
	void throwsExceptionIfBothDelimitersAreSimultaneouslySet() {
		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class,
			() -> provideArguments(",", ',', "", "foo"));

		assertThat(exception)//
				.hasMessageStartingWith("The delimiter and delimiterString attributes cannot be set simultaneously in")//
				.hasMessageContaining("CsvSource");
	}

	private Stream<Object[]> provideArguments(char delimiter, String emptyValue, String... value) {
		return provideArguments("", delimiter, emptyValue, value);
	}

	private Stream<Object[]> provideArguments(String delimiterString, String emptyValue, String... value) {
		return provideArguments(delimiterString, '\0', emptyValue, value);
	}

	private Stream<Object[]> provideArguments(String delimiterString, char delimiter, String emptyValue,
			String... value) {
		CsvSource annotation = mock(CsvSource.class);
		when(annotation.value()).thenReturn(value);
		when(annotation.delimiter()).thenReturn(delimiter);
		when(annotation.delimiterString()).thenReturn(delimiterString);
		when(annotation.emptyValue()).thenReturn(emptyValue);

		CsvArgumentsProvider provider = new CsvArgumentsProvider();
		provider.accept(annotation);
		return provider.provideArguments(null).map(Arguments::get);
	}

}
