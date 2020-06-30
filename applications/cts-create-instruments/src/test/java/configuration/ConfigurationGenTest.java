/*
 * Copyright 2014 Parity authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package configuration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class ConfigurationGenTest {

	private List<LocalDateTime> instrumentList;

	private String client1d2hConf;
	private String system1d2hConf;

	@BeforeAll
	void init() {
		instrumentList = Arrays.asList(LocalDateTime.parse("2020-06-11T00:00"),
				LocalDateTime.parse("2020-06-11T02:00"), LocalDateTime.parse("2020-06-11T04:00"),
				LocalDateTime.parse("2020-06-11T06:00"), LocalDateTime.parse("2020-06-11T08:00"),
				LocalDateTime.parse("2020-06-11T10:00"), LocalDateTime.parse("2020-06-11T12:00"),
				LocalDateTime.parse("2020-06-11T14:00"), LocalDateTime.parse("2020-06-11T16:00"),
				LocalDateTime.parse("2020-06-11T18:00"), LocalDateTime.parse("2020-06-11T20:00"),
				LocalDateTime.parse("2020-06-11T22:00"));

		try {
			client1d2hConf = new String(
					Files.readAllBytes(Paths.get("src/test/resources/client-1d2h.conf")));
			system1d2hConf = new String(
					Files.readAllBytes(Paths.get("src/test/resources/system-1d2h.conf")));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Test
	@DisplayName("1d2h interval instrument list")
	void populateInstrumentsTest() {
		LocalDateTime startDate = LocalDateTime.parse("2020-06-11T00:00");
		int interval = 120; // minutes
		int numDays = 1;

		List<LocalDateTime> actualInstruments =
				ConfigurationGenerator.populateInstruments(startDate, interval, numDays);
		assertEquals(instrumentList, actualInstruments);
	}

	@Test
	@DisplayName("1d2h client and system configuration output")
	void conf1d2hTest() {
		String actualClientOutput =
				ConfigurationGenerator.formatOutput(instrumentList, false).toString();
		String actualSystemOutput =
				ConfigurationGenerator.formatOutput(instrumentList, true).toString();

		assertAll(() -> assertEquals(client1d2hConf, actualClientOutput),
				() -> assertEquals(system1d2hConf, actualSystemOutput));

	}
}
