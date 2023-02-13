/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.controller.route

import android.health.connect.datatypes.ExerciseSessionRecord
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for reading an exercise route for the given session. */
@HiltViewModel
class ExerciseRouteViewModel
@Inject
constructor(private val loadExerciseRouteUseCase: LoadExerciseRouteUseCase) : ViewModel() {

    companion object {
        private const val TAG = "ExerciseRouteViewModel"
    }

    private val _exerciseSession = MutableLiveData<ExerciseSessionRecord?>()
    val exerciseSession: LiveData<ExerciseSessionRecord?>
        get() = _exerciseSession

    fun getExerciseWithRoute(sessionId: String) {
        viewModelScope.launch {
            when (val result = loadExerciseRouteUseCase.invoke(sessionId)) {
                is UseCaseResults.Success -> {
                    _exerciseSession.postValue(result.data)
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, result.exception.message!!)
                    _exerciseSession.postValue(null)
                }
            }
        }
    }
}
