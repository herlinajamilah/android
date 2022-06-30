package test.mega.privacy.android.app.domain.usecase

import android.util.Log
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.app.domain.usecase.CreateLogEntry
import mega.privacy.android.app.domain.usecase.CreateSdkLogEntry
import mega.privacy.android.app.domain.usecase.CreateTraceString
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock


@OptIn(ExperimentalCoroutinesApi::class)
class CreateSdkLogEntryTest {
    private lateinit var underTest: CreateLogEntry

    private val traceString = "Trace string"

    private val createTraceString = mock<CreateTraceString> {
        onBlocking { invoke(any(), any()) }.thenReturn(
            traceString)
    }

    @Before
    fun setUp() {
        underTest = CreateSdkLogEntry(createTraceString = createTraceString)
    }

    @Test
    fun `test that non null tag is used in result`() = runTest {
        val tag = "TAG"
        val request = CreateLogEntryRequest(
            tag = tag,
            message = "message",
            priority = Log.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.tag).isEqualTo(tag)
    }

    @Test
    fun `test that null tag uses client app tag`() = runTest {
        val clientAppTag = "[clientApp]"
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = Log.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.tag).isEqualTo(clientAppTag)
    }

    @Test
    fun `test that trace from loggers with no tag returns null`() = runTest {
        val loggingClass = "loggingClass"
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = Log.DEBUG,
            throwable = null,
            trace = listOf(StackTraceElement(loggingClass, "", "", 1)),
            loggingClasses = emptyList(),
            sdkLoggers = listOf(loggingClass),
        )
        val actual = underTest(request)

        assertThat(actual).isNull()
    }

    @Test
    fun `test that values match`() = runTest {
        val message = "message"
        val priority = Log.DEBUG
        val throwable = Throwable()
        val request = CreateLogEntryRequest(
            tag = null,
            message = message,
            priority = priority,
            throwable = throwable,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.message).isEqualTo(message)
        assertThat(actual?.priority).isEqualTo(priority)
        assertThat(actual?.throwable).isEqualTo(throwable)
    }

}