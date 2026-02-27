package com.khateev.hermi.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.khateev.hermi.R

class HermiSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return HermiMainScreen(carContext)
    }
}

class HermiMainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val rowEat = Row.Builder()
            .setTitle(carContext.getString(R.string.row_find_food_title))
            .addText(carContext.getString(R.string.row_find_food_subtitle))
            .setOnClickListener { /* AI Search Logic */ }
            .build()

        val rowCar = Row.Builder()
            .setTitle(carContext.getString(R.string.row_vehicle_status_title))
            .addText(carContext.getString(R.string.row_vehicle_status_subtitle))
            .setOnClickListener { /* VHAL Management Logic */ }
            .build()

        val listBuilder = ItemList.Builder()
            .addItem(rowEat)
            .addItem(rowCar)

        val voiceAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_mic))
                    .build()
            )
            .setOnClickListener {

            }
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle(carContext.getString(R.string.header_title))
            .setHeaderAction(Action.APP_ICON)
            .addAction(voiceAction)
            .build()
    }
}
