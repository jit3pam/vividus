/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.saucelabs;

import static com.github.valfirst.slf4jtest.LoggingEvent.debug;
import static com.github.valfirst.slf4jtest.LoggingEvent.error;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.JobsEndpoint;
import com.saucelabs.saucerest.model.jobs.Job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.selenium.cloud.AbstractCloudTestStatusManager.UpdateCloudTestStatusException;
import org.vividus.selenium.event.AfterWebDriverQuitEvent;
import org.vividus.ui.action.JavascriptActions;
import org.vividus.util.wait.DurationBasedWaiter;

@ExtendWith({MockitoExtension.class, TestLoggerFactoryExtension.class})
class SauceLabsTestStatusManagerTests
{
    private static final String SESSION_ID = "session123";
    private static final String LOG_MESSAGE = "Execution status of the SauceLabs job with ID '{}' is {}";
    private static final String TIMEOUT_ERROR = "Timeout occurred after waiting {} seconds for the SauceLabs job with"
            + " ID '{}' to complete";
    private static final long SESSION_COMPLETE_TIMEOUT_SECONDS = 1;

    private final TestLogger logger = TestLoggerFactory.getTestLogger(SauceLabsTestStatusManager.class);

    @Mock private JavascriptActions javascriptActions;
    @Mock private SauceREST sauceRestClient;
    private final DurationBasedWaiter waiter = new DurationBasedWaiter(
            Duration.ofSeconds(SESSION_COMPLETE_TIMEOUT_SECONDS), Duration.ofSeconds(2));
    private final AfterWebDriverQuitEvent afterWebDriverQuitEvent = new AfterWebDriverQuitEvent(SESSION_ID);
    private SauceLabsTestStatusManager manager;

    @BeforeEach
    void beforeEach()
    {
        manager = new SauceLabsTestStatusManager(mock(), mock(), javascriptActions, sauceRestClient, waiter);
    }

    @Test
    void shouldUpdateSessionStatus() throws UpdateCloudTestStatusException
    {
        manager.updateCloudTestStatus(null, "status");

        verify(javascriptActions).executeScript("sauce:job-result=status");
        verifyNoMoreInteractions(javascriptActions);
    }

    @Test
    void testSessionCompleted() throws IOException
    {
        String status = "complete";
        mockJobDetails(status);

        manager.waitForSessionCompletion(afterWebDriverQuitEvent);

        assertThat(logger.getLoggingEvents(), equalTo(List.of(debug(LOG_MESSAGE, SESSION_ID, status))));
    }

    @Test
    void testSessionIsNotCompleted() throws IOException
    {
        String status = "in-progress";
        mockJobDetails(status);

        manager.waitForSessionCompletion(afterWebDriverQuitEvent);

        assertThat(logger.getLoggingEvents(), equalTo(List.of(debug(LOG_MESSAGE, SESSION_ID, status),
                error(TIMEOUT_ERROR, SESSION_COMPLETE_TIMEOUT_SECONDS, SESSION_ID))));
    }

    private void mockJobDetails(String status) throws IOException
    {
        JobsEndpoint jobsEndpoint = mock();
        when(sauceRestClient.getJobsEndpoint()).thenReturn(jobsEndpoint);
        var jobDetails = new Job();
        jobDetails.status = status;
        when(jobsEndpoint.getJobDetails(SESSION_ID)).thenReturn(jobDetails);
    }

    @Test
    void testIsTestExecutionCompleteException() throws IOException
    {
        JobsEndpoint jobsEndpoint = mock();
        when(sauceRestClient.getJobsEndpoint()).thenReturn(jobsEndpoint);
        IOException ioException = new IOException("Network error");
        when(jobsEndpoint.getJobDetails(SESSION_ID)).thenThrow(ioException);

        manager.waitForSessionCompletion(afterWebDriverQuitEvent);
        assertThat(logger.getLoggingEvents(), equalTo(List.of(
                error(ioException, "Unable to get the status of the SauceLabs job  with ID '{}'", SESSION_ID),
                error(TIMEOUT_ERROR, SESSION_COMPLETE_TIMEOUT_SECONDS, SESSION_ID)
        )));
    }
}
