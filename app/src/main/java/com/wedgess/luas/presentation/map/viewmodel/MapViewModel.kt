package com.wedgess.luas.presentation.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.usecase.FetchAllStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import com.wedgess.luas.domain.usecase.IsLocationPermissionIgnoredUseCase
import com.wedgess.luas.domain.usecase.UpdateIgnoreLocationPermissionUseCase
import com.wedgess.luas.domain.usecase.UpdateLocationPermissionRequestedUseCase
import com.wedgess.luas.domain.usecase.WasLocationPermissionRequestedUseCase
import com.wedgess.luas.presentation.base.SideEffectViewModel
import com.wedgess.luas.presentation.base.SideEffectViewModelImpl
import com.wedgess.luas.presentation.extensions.toPermission
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.model.MapDialogState
import com.wedgess.luas.presentation.model.Permission
import com.wedgess.luas.presentation.model.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fetchCurrentLocationUseCase: FetchCurrentLocationUseCase,
    fetchAllStopsUseCase: FetchAllStopsUseCase,
    private val isLocationPermissionIgnoredUseCase: IsLocationPermissionIgnoredUseCase,
    private val wasLocationPermissionRequestedUseCase: WasLocationPermissionRequestedUseCase,
    private val updateLocationPermissionRequestedUseCase: UpdateLocationPermissionRequestedUseCase,
    private val updateIgnoreLocationPermissionUseCase: UpdateIgnoreLocationPermissionUseCase
) : ViewModel(), SideEffectViewModel<MapContract.Effect> by SideEffectViewModelImpl() {

    private val _uiState = MutableStateFlow(MapContract.UiState.initial())

    val uiResult = combine(
        _uiState,
        fetchCurrentLocationUseCase(),
        fetchAllStopsUseCase()
    ) { state, currentLocation, allStopsResult ->
        val allStops = allStopsResult.getOrDefault(emptyList())
        UiResult.Success(
            state.copy(
                currentLocation = currentLocation ?: UserLocation(0.0, 0.0),
                greenLineLocations = allStops.filter { it.line == LuasLineEntity.GREEN },
                redLineLocations = allStops.filter { it.line == LuasLineEntity.RED }
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResult.Loading)

    @OptIn(ExperimentalPermissionsApi::class)
    fun onEvent(event: MapContract.Event) {
        when (event) {
            MapContract.Event.OnAcceptPermissionClick -> {
                _uiState.update {
                    it.copy(dialogState = MapDialogState.None)
                }.also {
                    viewModelScope.emitSideEffect(MapContract.Effect.ShowSystemLocationPermissionDialog)
                }
            }

            is MapContract.Event.OnPermissionStateChanged -> handlePermissionChange(event.state)

            MapContract.Event.OnDismissPermissionClick -> _uiState.update {
                it.copy(dialogState = MapDialogState.None, locationPermission = Permission.Denied)
            }

            MapContract.Event.OnLocationPermanentlyDeniedDialog -> _uiState.update {
                it.copy(dialogState = MapDialogState.LocationPermissionPermanentlyDenied)
            }

            MapContract.Event.OnDismissDialogClick -> _uiState.update {
                it.copy(dialogState = MapDialogState.None)
            }

            MapContract.Event.OnIgnoreLocationPermissionClick -> _uiState.update {
                it.copy(dialogState = MapDialogState.None)
            }.also {
                viewModelScope.launch {
                    updateIgnoreLocationPermissionUseCase(true)
                }
            }

            MapContract.Event.OnOpenAppSettingsPermissionClick -> _uiState.update {
                it.copy(dialogState = MapDialogState.None)
            }.also {
                viewModelScope.emitSideEffect(
                    MapContract.Effect.OpenAppPermissionScreen
                )
            }

            MapContract.Event.OnLocationWasRequested -> viewModelScope.launch {
                updateLocationPermissionRequestedUseCase(true)
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    private fun handlePermissionChange(permissionState: MultiplePermissionsState) {
        viewModelScope.launch {
            val wasLocationPermissionRequested = wasLocationPermissionRequestedUseCase()
            val ignoreLocationPermission = isLocationPermissionIgnoredUseCase()
            val permission = permissionState.toPermission(wasLocationPermissionRequested)
            if (permission == Permission.Granted) {
                if (ignoreLocationPermission) {
                    updateIgnoreLocationPermissionUseCase(false)
                }
                fetchCurrentLocationUseCase.refresh()
            }
            Timber.d(
                "Location, wasLocationPermissionRequested: " +
                    "$wasLocationPermissionRequested, ignoreLocationPermission: " +
                    "$ignoreLocationPermission, permission: $permission"
            )
            _uiState.update {
                it.copy(
                    locationPermission = permission,
                    dialogState = when (permission) {
                        Permission.ShowRationale -> MapDialogState.LocationPermissionRationale
                        Permission.PermanentlyDenied -> {
                            if (ignoreLocationPermission) {
                                MapDialogState.None
                            } else {
                                MapDialogState.LocationPermissionPermanentlyDenied
                            }
                        }

                        else -> it.dialogState
                    }
                )
            }
        }
    }
}
