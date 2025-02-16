package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.RefreshFlow
import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FetchForecastUseCase @Inject constructor(private val luasRepository: LuasRepository) {

    private val refreshFlow: RefreshFlow = RefreshFlow()
    private var refreshMode = RefreshMode.AUTOMATIC

    operator fun invoke(stopAbv: String): Flow<Result<ForcastEntity>> {
        return refreshFlow.flatMapLatest {
            flow {
                when (refreshMode) {
                    RefreshMode.AUTOMATIC -> {
                        val result = luasRepository.fetchForecast(stopAbv)
                        emit(result)
                        delay(REFRESH_INTERVAL)
                        refreshFlow.refresh()
                    }

                    RefreshMode.MANUAL -> {
                        val result = luasRepository.fetchForecast(stopAbv)
                        emit(result)
                        if (result.isSuccess) {
                            refreshMode = RefreshMode.AUTOMATIC
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
        private const val REFRESH_INTERVAL = 20_000L
    }
}
