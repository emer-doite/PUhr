package PUhr.clock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class ClockMode {
    data object AnalogDigital : ClockMode()
}

@HiltViewModel
class ClockViewModel @Inject constructor() : ViewModel() {
    private val _mode = MutableStateFlow<ClockMode>(ClockMode.AnalogDigital)
    val mode: StateFlow<ClockMode> = _mode.asStateFlow()
}
