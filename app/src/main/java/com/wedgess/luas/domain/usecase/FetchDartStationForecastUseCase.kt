package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.RefreshFlow
import com.wedgess.luas.domain.model.DartStationForecastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.repository.DartRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FetchDartStationForecastUseCase @Inject constructor(private val dartRepository: DartRepository) {

    private val refreshFlow: RefreshFlow = RefreshFlow()
    private var refreshMode = RefreshMode.AUTOMATIC

    operator fun invoke(stationCode: String): Flow<RefreshState<List<DartStationForecastEntity>>> {
        return refreshFlow.flatMapLatest {
            flow {
                when (refreshMode) {
                    RefreshMode.AUTOMATIC -> {
                        val result = dartRepository.fetchForecast(stationCode)
                        result.onSuccess { forecast ->
                            emit(RefreshState.Success(data = forecast, progress = 0f))
                            val interval = REFRESH_INTERVAL / FULL_PERCENTAGE
                            for (i in 1..FULL_PERCENTAGE) {
                                val percentageBeforeRefresh = i / FULL_PERCENTAGE.toFloat()
                                emit(
                                    RefreshState.Success(
                                        data = forecast,
                                        progress = percentageBeforeRefresh,
                                    ),
                                )
                                delay(interval)
                            }
                            refreshFlow.refresh()
                        }.onFailure {
                            emit(
                                RefreshState.Error(
                                    result.exceptionOrNull() ?: Exception("Unknown error"),
                                ),
                            )
                        }
                    }

                    RefreshMode.MANUAL -> {
                        val result = dartRepository.fetchForecast(stationCode)
                        result.onSuccess { forecast ->
                            emit(RefreshState.Success(data = forecast, progress = 100f))
                            refreshMode = RefreshMode.AUTOMATIC
                            refreshFlow.refresh()
                        }.onFailure { throwable ->
                            emit(RefreshState.Error(throwable))
                        }
                    }
                }
            }
        }
    }

    fun refresh(refreshMode: RefreshMode = RefreshMode.MANUAL) {
        this.refreshMode = refreshMode
        refreshFlow.refresh()
    }

    companion object {
        private const val REFRESH_INTERVAL = 60_000L
        private const val FULL_PERCENTAGE = 100
    }
}
