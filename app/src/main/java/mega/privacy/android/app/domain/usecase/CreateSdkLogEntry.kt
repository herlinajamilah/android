package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.app.domain.entity.logging.LogEntry
import javax.inject.Inject

/**
 * Create sdk log entry implementation of [CreateLogEntry]
 *
 */
class CreateSdkLogEntry @Inject constructor(
    private val createTraceString: CreateTraceString,
) : CreateLogEntry {
    override suspend fun invoke(request: CreateLogEntryRequest): LogEntry? {
        return when {
            isSdkLog(request.tag) -> LogEntry(request.tag,
                request.message,
                null,
                request.priority,
                request.throwable)
            isNotSdkLog(request.trace, request.sdkLoggers) -> LogEntry(
                "[clientApp]",
                request.message,
                createTraceString(request.trace, request.loggingClasses),
                request.priority,
                request.throwable
            )
            else -> null
        }
    }

    private fun isSdkLog(tag: String?) = tag != null

    private fun isNotSdkLog(trace: List<StackTraceElement>, sdkLoggers: List<String>) =
        trace.none { it.className in sdkLoggers }

}