package com.hx.oxidemod

object Features {

    sealed class MenuElement {
        abstract val name: String
        data class SwitchItem(override val name: String, var isChecked: Boolean = false, val action: (Boolean) -> Unit = {}) : MenuElement()
        data class SliderItem(override val name: String, var value: Float, val min: Float, val max: Float, val action: (Float) -> Unit = {}) : MenuElement()
    }

    data class SubCategory(val name: String, val elements: MutableList<MenuElement>)
    data class Category(val name: String, val icon: String, val subs: List<SubCategory>)

    val menuData = mutableListOf(
        Category("Візуал", "👁️", listOf(
            SubCategory("Гравці", mutableListOf(
                MenuElement.SwitchItem("Бокс (ESP)", false) { PatchEngine.visuals.box = it },
                MenuElement.SwitchItem("Скелет", false) { PatchEngine.visuals.skeleton = it },
                MenuElement.SwitchItem("Імена", false) { PatchEngine.visuals.name = it },
                MenuElement.SwitchItem("Здоров'я", false) { PatchEngine.visuals.health = it },
                MenuElement.SliderItem("Дальність", 150f, 50f, 500f) { PatchEngine.visuals.distance = it }
            )),
            SubCategory("Стиль", mutableListOf(
                MenuElement.SwitchItem("Лінії", false) { PatchEngine.visuals.line = it },
                MenuElement.SliderItem("Товщина ліній", 1f, 0.5f, 3f) { _ -> }
            ))
        )),
        Category("Світ", "🌍", listOf(
            SubCategory("Руда", mutableListOf(
                MenuElement.SwitchItem("Камінь", false) { },
                MenuElement.SwitchItem("Метал", false) { },
                MenuElement.SwitchItem("Сірка", false) { }
            )),
            SubCategory("Бази", mutableListOf(
                MenuElement.SwitchItem("Шафи", false) { },
                MenuElement.SwitchItem("Двері", false) { },
                MenuElement.SwitchItem("Турелі", false) { }
            )),
            SubCategory("Транспорт", mutableListOf(
                MenuElement.SwitchItem("Машини", false) { },
                MenuElement.SwitchItem("Вертольоти", false) { }
            ))
        )),
        Category("Гравець", "🎯", listOf(
            SubCategory("Основне", mutableListOf(
                MenuElement.SwitchItem("God Mode", false) { PatchEngine.player.health = it },
                MenuElement.SwitchItem("Нескінченні патрони", false) { PatchEngine.player.ammo = it },
                MenuElement.SliderItem("Швидкість", 1.0f, 1.0f, 5.0f) { PatchEngine.player.speed = it }
            )),
            SubCategory("Зброя", mutableListOf(
                MenuElement.SwitchItem("Без віддачі", false) { PatchEngine.weapon.noRecoil = it },
                MenuElement.SwitchItem("Без розкиду", false) { PatchEngine.weapon.noSpread = it }
            ))
        )),
        Category("Налаштування", "⚙️", listOf(
            SubCategory("Конфіги", mutableListOf(
                MenuElement.SwitchItem("Зберегти", false) { },
                MenuElement.SwitchItem("Завантажити", false) { }
            ))
        ))
    )
}
