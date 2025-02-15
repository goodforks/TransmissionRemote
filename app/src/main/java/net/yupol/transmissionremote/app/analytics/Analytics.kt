package net.yupol.transmissionremote.app.analytics

import com.google.firebase.analytics.FirebaseAnalytics.Event
import com.google.firebase.analytics.FirebaseAnalytics.Param
import net.yupol.transmissionremote.app.transport.request.Request
import javax.inject.Inject

class Analytics @Inject constructor(
    private val analyticsProvider: AnalyticsProvider
) {
    fun logScreenView(screenName: String, screenClass: Class<*>) {
        analyticsProvider.logEvent(
            name = Event.SCREEN_VIEW,
            Param.SCREEN_NAME to screenName,
            Param.SCREEN_CLASS to screenClass.simpleName
        )
    }

    fun <T> logOkHttpRequestSuccess(
        requestClass: Class<T>,
        responseTimeMillis: Long
    ) where T : Request<*> {
        analyticsProvider.logEvent(
            name = EVENT_OKHTTP_REQUEST_SUCCESS,
            PARAM_REQUEST_CLASS to requestClass.simpleName,
            PARAM_RESPONSE_TIME_MILLIS to "$responseTimeMillis"
        )
    }

    fun <T> logOkHttpRequestFailure(
        requestClass: Class<T>,
        responseTimeMillis: Long
    ) where T : Request<*> {
        analyticsProvider.logEvent(
            name = EVENT_OKHTTP_REQUEST_FAILURE,
            PARAM_REQUEST_CLASS to requestClass.simpleName,
            PARAM_RESPONSE_TIME_MILLIS to "$responseTimeMillis"
        )
    }

    fun <T> logRobospiceRequestSuccess(
        requestClass: Class<T>,
        responseTimeMillis: Long
    ) where T : Request<*> {
        analyticsProvider.logEvent(
            name = EVENT_ROBOSPICE_REQUEST_SUCCESS,
            PARAM_REQUEST_CLASS to requestClass.simpleName,
            PARAM_RESPONSE_TIME_MILLIS to "$responseTimeMillis"
        )
    }

    fun <T> logRobospiceRequestFailure(
        requestClass: Class<T>,
        responseTimeMillis: Long
    ) where T : Request<*> {
        analyticsProvider.logEvent(
            name = EVENT_ROBOSPICE_REQUEST_FAILURE,
            PARAM_REQUEST_CLASS to requestClass.simpleName,
            PARAM_RESPONSE_TIME_MILLIS to "$responseTimeMillis"
        )
    }

    fun setTorrentsCount(count: Int) {
        analyticsProvider.setUserProperty(PROPERTY_TORRENTS_COUNT, TorrentCount.fromCount(count).value)
    }

    fun logStartupTimeSLI(timeMillis: Long) {
        analyticsProvider.logEvent(
            name = EVENT_SLI_STARTUP_TIME,
            PROPERTY_STARTUP_TIME_MILLIS to timeMillis
        )
    }

    companion object {
        private const val PROPERTY_TORRENTS_COUNT = "torrents_count"
        private const val PROPERTY_STARTUP_TIME_MILLIS = "startup_time_millis"

        private const val EVENT_OKHTTP_REQUEST_SUCCESS = "okhttp_request_success"
        private const val EVENT_OKHTTP_REQUEST_FAILURE = "okhttp_request_failure"

        private const val EVENT_ROBOSPICE_REQUEST_SUCCESS = "robospice_request_success"
        private const val EVENT_ROBOSPICE_REQUEST_FAILURE = "robospice_request_failure"

        private const val EVENT_SLI_STARTUP_TIME = "sli_startup_time"

        private const val PARAM_REQUEST_CLASS = "request_class"
        private const val PARAM_RESPONSE_TIME_MILLIS = "response_time_millis"
    }
}
