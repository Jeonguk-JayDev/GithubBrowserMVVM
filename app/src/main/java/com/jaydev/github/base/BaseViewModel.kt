package com.jaydev.github.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jaydev.github.domain.NetResult
import com.jaydev.github.domain.entity.NetError
import com.jaydev.github.model.PopupMessage
import kotlinx.coroutines.flow.*

abstract class BaseViewModel : ViewModel() {
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _showToast = MutableLiveData<String>()
    val showToast: LiveData<String> = _showToast

    private val _showAlertDialog = MutableLiveData<PopupMessage>()
    val showAlertDialog: LiveData<PopupMessage> = _showAlertDialog

    private val _showRetryDialog = MutableLiveData<Retry>()
    val showRetryDialog: LiveData<Retry> = _showRetryDialog

    protected fun showToast(message: String) {
        _showToast.value = message
    }

    protected fun showAlertDialog(title: String, message: String) {
        _showAlertDialog.value = PopupMessage(title, message)
    }

    protected fun showAlertDialog(popup: PopupMessage) {
        _showAlertDialog.value = popup
    }

    protected fun <T> Flow<NetResult<T>>.onSuccess(
        success: (suspend (T) -> Unit)? = null
    ) = onEach {
        if (it is NetResult.Success) success?.invoke(it.response)
    }

    protected fun <T> Flow<NetResult<T>>.onFailure(
        failure: (suspend (NetError.BadRequest) -> Unit)? = null
    ) = onEach {
        if (it is NetResult.Error && it.error is NetError.BadRequest)
            failure?.invoke(it.error as NetError.BadRequest)
    }

    protected fun <T> Flow<NetResult<T>>.commonErrorHandler(
        retryAction: RetryInvokable? = null
    ) = onEach {
        handleError(it, retryAction)
    }

    protected suspend fun <T> Flow<NetResult<T>>.call() = collect()

    protected suspend fun <T> Flow<NetResult<T>>.load(
        loading: ((Boolean) -> Unit)? = null
    ) = onStart {
        if (loading != null) loading.invoke(true) else _loading.value = true
    }.onCompletion {
        if (loading != null) loading.invoke(false) else _loading.value = false
    }.collect()

    private fun handleError(result: NetResult<*>, action: RetryInvokable?) {
        if (result !is NetResult.Error) return

        when (result.error) {
            NetError.Network -> _showToast.value = "???????????? ?????? ??????"
            is NetError.InternalServer -> {

                _showRetryDialog.value =
                    Retry(action, PopupMessage("?????? ?????? ", "???????????? ?????? ??????."))
            }
            is NetError.Timeout -> {
                _showRetryDialog.value =
                    Retry(action, PopupMessage("???????????? ??????", "????????? ?????? ??????."))
            }
            is NetError.Unknown -> {
                _showAlertDialog.value =
                    PopupMessage("?????? ?????? ??????", "??? ??? ?????? ????????? ????????????.\n?????? ?????? ???????????? ?????? ??????.")
            }
        }
    }
}
