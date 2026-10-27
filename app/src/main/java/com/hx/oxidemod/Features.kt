package com.hx.oxidemod

// Тут ми описуємо всі вкладки та функції.
// Щоб додати нову функцію - просто допиши рядок у потрібну вкладку.
object Features {
    
    data class Item(
        val name: String,
        var isEnabled: Boolean = false,
        val action: (Boolean) -> Unit = {} // Тут буде логіка
    )

    data class Category(
        val name: String,
        val icon: String, // використовуємо емодзі замість іконок для простоти
        val items: MutableList<Item>
    )

    val menuData = mutableListOf(
        Category("Візуал", "👁️", mutableListOf(
            Item("Бокс (ESP)", false) { enabled -> PatchEngine.visuals.box = enabled },
            Item("Лінії", false) { enabled -> PatchEngine.visuals.line = enabled },
            Item("Скелет", false) { enabled -> PatchEngine.visuals.skeleton = enabled },
            Item("Імена", false) { enabled -> PatchEngine.visuals.name = enabled },
            Item("Дистанція", false) { enabled -> PatchEngine.visuals.distance = enabled }
        )),
        Category("Світ", "🌍", mutableListOf(
            Item("Час (День)", false) { enabled -> PatchEngine.world.day = enabled },
            Item("Максимальний лут", false) { enabled -> PatchEngine.world.loot = enabled }
        )),
        Category("Гравець", "🎯", mutableListOf(
            Item("God Mode", false) { enabled -> PatchEngine.player.health = enabled },
            Item("Нескінченні патрони", false) { enabled -> PatchEngine.player.ammo = enabled },
            Item("Швидкість x3", false) { enabled -> PatchEngine.player.speed = enabled }
        )),
        Category("Налаштування", "⚙️", mutableListOf(
            Item("Зберегти конфіг", false) { _ -> /* логіка збереження */ },
            Item("Загрузити конфіг", false) { _ -> /* логіка загрузки */ }
        ))
    )
}
