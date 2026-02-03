package com.khateev.hermi.repository

import android.car.Car
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.khateev.hermi.mapPropertyToState
import com.khateev.hermi.readStaticValue
import com.khateev.hermi.shared.VhalParamsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class VhalReaderImpl @Inject constructor(private val context: Context): VhalReader {
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    private val _carState = MutableStateFlow(VhalParamsState.Companion.initial)
    override val carState: StateFlow<VhalParamsState> = _carState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            _carState.update { currentState ->
                mapPropertyToState(value, currentState)
            }
        }

        override fun onErrorEvent(propId: Int, area: Int) {
            Log.e("VHAL_LIB", "Error on property $propId in area $area")
        }
    }

    override fun connect() {
        car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { carInstance, ready ->
            if (ready) {
                propertyManager = carInstance.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                registerSensors()
            }
        }
        _isConnected.value = true
    }

    private fun registerSensors() {
        val manager = propertyManager ?: return
        val configs = manager.propertyList

        configs.forEach { config ->
            val id = config.propertyId

            if (config.changeMode != CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_STATIC) {
                try {
                    manager.registerCallback(
                        propertyCallback,
                        id,
                        CarPropertyManager.SENSOR_RATE_ONCHANGE
                    )
                } catch (e: SecurityException) {
                    Log.e("VHAL", "No permission to register property: $id, error: $e")
                }
            } else {
                readStaticValue(propertyManager, id, _carState)
            }
        }
    }

    override fun disconnect() {
        propertyManager?.unregisterCallback(propertyCallback)
        car?.disconnect()
        _isConnected.value = false
    }
}