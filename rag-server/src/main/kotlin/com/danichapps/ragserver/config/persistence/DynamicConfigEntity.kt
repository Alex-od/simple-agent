package com.danichapps.ragserver.config.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "dynamic_config")
class DynamicConfigEntity(
    @Id
    @Column(name = "config_key", nullable = false, length = 255)
    val key: String = "",

    @Column(name = "config_value", nullable = false, length = 4096)
    var value: String = ""
)
