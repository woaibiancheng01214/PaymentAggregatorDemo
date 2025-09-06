package com.example.payagg.infra

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["com.example.payagg.ports"])
@EntityScan(basePackages = ["com.example.payagg.domain"])
@EnableJpaAuditing
@EnableTransactionManagement
class DatabaseConfig
