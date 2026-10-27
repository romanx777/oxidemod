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
                MenuElement.SwitchItem("Бокс (ESP)", false) { PatchEngine.visuals.box = it },
                MenuElement.SwitchItem("Скелет", false) { PatchEngine.visuals.skeleton = it },
                MenuElement.SwitchItem("Імена", false) { PatchEngine.visuals.name = it },
                MenuElement.SliderItem("Дальність", 150f, 50f, 500f) { PatchEngine.visuals.distance = it }
            ))
        )),
        Category("СВІТ", "WLD", listOf(
            SubCategory("Лут", mutableListOf(
                MenuElement.SwitchItem("Камінь", false) { },
                MenuElement.SwitchItem("Метал", false) { },
                MenuElement.SwitchItem("Сірка", false) { }
            ))
        )),
        Category("ГРАВЕЦЬ", "PLR", listOf(
            SubCategory("Основне", mutableListOf(
                MenuElement.SwitchItem("God Mode", false) { PatchEngine.player.health = it },
                MenuElement.SwitchItem("Нескінченні патрони", false) { PatchEngine.player.ammo = it },
                MenuElement.SliderItem("Швидкість", 1.0f, 1.0f, 5.0f) { PatchEngine.player.speed = it }
            ))
        )),
        Category("АІМБОТ", "AIM", listOf(
            SubCategory("Налаштування", mutableListOf(
                MenuElement.SwitchItem("Аімбот", false) { PatchEngine.aimbot.enabled = it },
                MenuElement.SwitchItem("Silent Aim", false) { PatchEngine.aimbot.silent = it },
                MenuElement.SwitchItem("Малювати FOV", false) { PatchEngine.aimbot.drawFov = it },
                MenuElement.SliderItem("Радіус FOV", 150f, 50f, 500f) { PatchEngine.aimbot.fov = it },
                MenuElement.SliderItem("Плавність", 2.0f, 1.0f, 10.0f) { PatchEngine.aimbot.smooth = it }
            )),
            SubCategory("Ціль", mutableListOf(
                MenuElement.SwitchItem("Голова", false) { PatchEngine.aimbot.targetHead = it },
                MenuElement.SwitchItem("Тіло", false) { PatchEngine.aimbot.targetBody = it }
            ))
        ))
    )
}
