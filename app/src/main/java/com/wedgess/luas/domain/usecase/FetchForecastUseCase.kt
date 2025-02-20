package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.RefreshFlow
import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FetchForecastUseCase @Inject constructor(private val luasRepository: LuasRepository) {

    private val refreshFlow: RefreshFlow = RefreshFlow()
    private var refreshMode = RefreshMode.AUTOMATIC

    operator fun invoke(stopAbv: String): Flow<RefreshState<ForcastEntity>> {
        return refreshFlow.flatMapLatest {
            flow {
                when (refreshMode) {
                    RefreshMode.AUTOMATIC -> {
                        val result = luasRepository.fetchForecast(stopAbv)
                        result.onSuccess { forecast ->
                            emit(RefreshState.Success(data = forecast, progress = 0f))
                            val interval = REFRESH_INTERVAL / 100
                            for (i in 1..100) {
                                val percentageBeforeRefresh = i / 100f
                                emit(
                                    RefreshState.Success(
                                        data = forecast,
                                        progress = percentageBeforeRefresh
                                    )
                                )
                                delay(interval)
                            }
                            refreshFlow.refresh()
                        }.onFailure {
                            emit(
                                RefreshState.Error(
                                    result.exceptionOrNull() ?: Exception("Unknown error")
                                )
                            )
                        }

                    }

                    RefreshMode.MANUAL -> {
                        val result = luasRepository.fetchForecast(stopAbv)
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
        private const val REFRESH_INTERVAL = 15_000L
    }
}
