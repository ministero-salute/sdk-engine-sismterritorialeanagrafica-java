package it.mds.sdk.flusso.sism.territoriale.anag;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"it.mds.sdk.flusso.sism.territoriale.anag.controller", "it.mds.sdk.flusso.sism.territoriale.anag", "it.mds.sdk.rest.persistence.entity",
        "it.mds.sdk.libreriaregole.validator",
        "it.mds.sdk.flusso.sism.territoriale.anag.service", "it.mds.sdk.flusso.sism.territoriale.anag.tracciato",
        "it.mds.sdk.gestoreesiti", "it.mds.sdk.flusso.sism.territoriale.anag.parser.regole", "it.mds.sdk.flusso.sism.territoriale.anag.parser.regole.conf",
        "it.mds.sdk.connettoremds"})

@OpenAPIDefinition(info = @Info(title = "SDK Ministero Della Salute - Flusso ANT", version = "0.0.5-SNAPSHOT", description = "Flusso Sism Territoriale Anagrafica"))
public class FlussoSismTerritorialeAnag {
    public static void main(String[] args) {
        SpringApplication.run(FlussoSismTerritorialeAnag.class, args);
    }

}
