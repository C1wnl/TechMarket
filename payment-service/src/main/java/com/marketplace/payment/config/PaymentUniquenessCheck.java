package com.marketplace.payment.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.*;

// Hibernate update puede registrar un error de DDL y continuar: no aceptar pagos sin UNIQUE.
@Component
public class PaymentUniquenessCheck implements ApplicationRunner {
    private final DataSource dataSource;
    public PaymentUniquenessCheck(DataSource dataSource) { this.dataSource = dataSource; }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            String table = metadata.storesUpperCaseIdentifiers() ? "PAYMENTS" : "payments";
            var indexes = new HashMap<String, List<String>>();
            try (var rows = metadata.getIndexInfo(connection.getCatalog(), connection.getSchema(), table, true, false)) {
                while (rows.next()) {
                    String name = rows.getString("INDEX_NAME"), column = rows.getString("COLUMN_NAME");
                    if (name != null && column != null && !rows.getBoolean("NON_UNIQUE"))
                        indexes.computeIfAbsent(name, ignored -> new ArrayList<>()).add(column.toLowerCase(Locale.ROOT));
                }
            }
            if (indexes.values().stream().noneMatch(columns -> columns.equals(List.of("order_id"))))
                throw new IllegalStateException("Falta UNIQUE(order_id) en payments. Aplicar sql/01-payment-order-unique.sql y revisar duplicados históricos.");
        }
    }
}
