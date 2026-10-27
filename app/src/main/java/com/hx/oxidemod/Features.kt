package com.hx.oxidemod

object Features {
    sealed class MenuElement {
        abstract val name: String
        data class SwitchItem(override val name: String, var isChecked: Boolean = false, val action: (Boolean) -> Unit = {}) : MenuElement()
        data class SliderItem(override val name: String, var value: Float, val min: Float, val max: Float, val action: (Float) -> Unit = {}) : MenuElement()
    }

    data class SubCategory(val name: String, val elements: MutableList<MenuElement>)
    data class Category(val name: String, val tag: String, val subs: List<SubCategory>)

    val menuData = mutableListOf(
        Category("ВІЗУАЛ", "VIS", listOf(
            SubCategory("Гравці", mutableListOf(
                MenuElement.SwitchItem("Бокс (ESP)", false) { PatchEngine.visuals.box = it }
            )),
            SubCategory("Приціл", mutableListOf(
                MenuElement.SwitchItem("Малювати FOV", false) { PatchEngine.visuals.fovEnabled = it },
                MenuElement.SliderItem("Радіус FOV", 200f, 50f, 800f) { PatchEngine.visuals.fovRadius = it }
            ))
        )),
        Category("ГРАВЕЦЬ", "PLR", listOf(
            SubCategory("Основне", mutableListOf(
                MenuElement.SwitchItem("God Mode", false) { PatchEngine.player.health = it },
                MenuElement.SwitchItem("Патрони", false) { PatchEngine.player.ammo = it }
            ))
        ))
    )
}
