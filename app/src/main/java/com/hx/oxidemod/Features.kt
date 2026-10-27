package com.hx.oxidemod

object Features {

    sealed class MenuElement {
        abstract val name: String
        data class SwitchItem(override val name: String, var isChecked: Boolean = false, val action: (Boolean) -> Unit = {}) : MenuElement()
        data class SliderItem(override val name: String, var value: Float, val min: Float, val max: Float, val action: (Float) -> Unit = {}) : MenuElement()
    }

    data class Category(
        val name: String,
        val icon: String,
        val elements: MutableList<MenuElement>
    )

    val menuData = mutableListOf(
        Category("Візуал", "👁️", mutableListOf(
            MenuElement.SwitchItem("Бокс (ESP)", false) { PatchEngine.visuals.box = it },
            MenuElement.SwitchItem("Лінії", false) { PatchEngine.visuals.line = it },
            MenuElement.SwitchItem("Скелет", false) { PatchEngine.visuals.skeleton = it },
            MenuElement.SwitchItem("Імена", false) { PatchEngine.visuals.name = it },
            MenuElement.SliderItem("Дальність (м)", 150f, 50f, 500f) { PatchEngine.visuals.distance = it }
        )),
        Category("Гравець", "🎯", mutableListOf(
            MenuElement.SwitchItem("God Mode", false) { PatchEngine.player.health = it },
            MenuElement.SwitchItem("Нескінченні патрони", false) { PatchEngine.player.ammo = it },
            MenuElement.SliderItem("Множник швидкості", 1.0f, 1.0f, 5.0f) { PatchEngine.player.speed = it }
        )),
        Category("Зброя", "🔫", mutableListOf(
            MenuElement.SwitchItem("Без віддачі", false) { PatchEngine.weapon.noRecoil = it },
            MenuElement.SwitchItem("Без розкиду", false) { PatchEngine.weapon.noSpread = it }
        )),
        Category("Налаштування", "⚙️", mutableListOf(
            MenuElement.SwitchItem("Зберегти конфіг", false) { /* логіка */ },
            MenuElement.SwitchItem("Завантажити конфіг", false) { /* логіка */ }
        ))
    )
}
