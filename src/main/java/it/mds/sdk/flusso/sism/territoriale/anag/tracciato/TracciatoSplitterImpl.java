package it.mds.sdk.flusso.sism.territoriale.anag.tracciato;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.mds.sdk.flusso.sism.territoriale.anag.parser.regole.RecordDtoSismTerritorialeAnag;
import it.mds.sdk.flusso.sism.territoriale.anag.parser.regole.conf.ConfigurazioneFlussoSismTerritorialeAnag;
import it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.ObjectFactory;
import it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.PeriodoRiferimento;
import it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.TerritorialeAnagrafica;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.tracciato.TracciatoSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("tracciatoSplitterSismTerrAnag")
public class TracciatoSplitterImpl implements TracciatoSplitter<RecordDtoSismTerritorialeAnag> {

    @Override
    public List<Path> dividiTracciato(Path tracciato) {
        return null;
    }

    @Override
    public List<Path> dividiTracciato(List<RecordDtoSismTerritorialeAnag> records, String idRun) {

        try {
            ConfigurazioneFlussoSismTerritorialeAnag conf = getConfigurazione();
            String annoRif = records.get(0).getAnnoRiferimento();
            String codiceRegione = records.get(0).getCodiceRegione();
            //XML ANAGRAFICA
            ObjectFactory objAnag = getObjectFactory();
            TerritorialeAnagrafica territorialeAnagrafica = objAnag.createTerritorialeAnagrafica();

            territorialeAnagrafica.setCodiceRegione(codiceRegione);
            territorialeAnagrafica.setAnnoRiferimento(annoRif);
            territorialeAnagrafica.setPeriodoRiferimento(it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));

            for (RecordDtoSismTerritorialeAnag r : records) {
                if (!r.getTipoOperazioneAnagrafica().equalsIgnoreCase("NM")) {
                    creaAnagraficaXml(r, territorialeAnagrafica, objAnag);
                }
            }

            //recupero il path del file xsd di anagrafica
            URL resourceAnagrafica = this.getClass().getClassLoader().getResource("ANT.xsd");
            log.debug("URL dell'XSD per la validazione idrun {} : {}", idRun, resourceAnagrafica);

            //scrivi XML ANAGRAFICA
            GestoreFile gestoreFile = GestoreFileFactory.getGestoreFile("XML");

            String pathAnagraf = conf.getXmlOutput().getPercorso() + "SDK_TER_ANT_" + records.get(0).getPeriodoRiferimento() + "_" + idRun + ".xml";
            //gestoreFile.scriviDto(territorialeAnagrafica, pathAnagraf, resourceAnagrafica);


            return List.of(Path.of(pathAnagraf));
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            log.error("[{}].dividiTracciato  - records[{}]  - idRun[{}] -" + e.getMessage(),
                    this.getClass().getName(),
                    records.stream().map(obj -> "" + obj.toString()).collect(Collectors.joining("|")),
                    idRun,
                    e
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossibile validare il csv in ingresso. message: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void creaAnagraficaXml(RecordDtoSismTerritorialeAnag r, TerritorialeAnagrafica territorialeAnagrafica,
                                   ObjectFactory objAnag) {
        //ASL RIF
        TerritorialeAnagrafica.AziendaSanitariaRiferimento currentAsl = getCurrentAsl(territorialeAnagrafica, r);
        if (currentAsl == null) {
            currentAsl = creaAsl(r.getCodiceAziendaSanitariaRiferimento(), objAnag);
            territorialeAnagrafica.getAziendaSanitariaRiferimento().add(currentAsl);
        }

        //DSM
        TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM currentDsm = getCurrentDsm(currentAsl, r);
        if (currentDsm == null) {
            currentDsm = creaDSM(r.getCodiceDipartimentoSaluteMentale(), objAnag);
            currentAsl.getDSM().add(currentDsm);
        }

        //ASSISTITO
        TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito = creaAssistito(r, objAnag);
        currentDsm.getAssistito().add(currentAssisitito);
    }

    private TerritorialeAnagrafica.AziendaSanitariaRiferimento creaAsl(String codAsl,
                                                                       ObjectFactory objAnag) {
        TerritorialeAnagrafica.AziendaSanitariaRiferimento asl = objAnag.createTerritorialeAnagraficaAziendaSanitariaRiferimento();
        asl.setCodiceAziendaSanitariaRiferimento(codAsl);
        return asl;
    }

    private TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM creaDSM(String codDsm,
                                                                           ObjectFactory objAnag) {
        TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM dsm = objAnag.createTerritorialeAnagraficaAziendaSanitariaRiferimentoDSM();
        dsm.setCodiceDSM(codDsm);
        return dsm;
    }

    private TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM.Assistito creaAssistito(RecordDtoSismTerritorialeAnag r,
                                                                                           ObjectFactory objAnag) {
        TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM.Assistito assistito = objAnag.createTerritorialeAnagraficaAziendaSanitariaRiferimentoDSMAssistito();
        assistito.setSesso(r.getSesso());
        assistito.setAnnoNascita(r.getAnnoNascita());
        assistito.setASLResidenza(r.getAslProvenienza());
        assistito.setCittadinanza(r.getCittadinanza());
        assistito.setCodiceProfessionale(r.getCodiceProfessionale());
        assistito.setCodiceRegioneResidenza(r.getCodiceRegioneResidenza());
        assistito.setCollocazioneSocioAmbientale(r.getCollocazioneSocioAmbientale());
        assistito.setCUNI(r.getCuni());
        assistito.setIdRec(r.getIdRecord());
        assistito.setStatoCivile(r.getStatoCivile());
        assistito.setStatoEsteroResidenza(r.getStatoEsteroResidenza());
        assistito.setTipologiaCI(BigInteger.valueOf(r.getTipologiaCodiceIdentificativoAssistito()));
        assistito.setValiditaCI(BigInteger.valueOf(r.getValiditaCodiceIdentificativoAssistito()));
        assistito.setTipoOperazione(it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.TipoOperazione.fromValue(r.getTipoOperazioneAnagrafica()));
        assistito.setTitoloStudio(r.getTitoloStudio());
        return assistito;
    }

    public TerritorialeAnagrafica creaTerritorialeAnagrafica(List<RecordDtoSismTerritorialeAnag> records, TerritorialeAnagrafica territorialeAnagrafica) {

        //Imposto gli attribute element
        String annoRif = records.get(0).getAnnoRiferimento();
        String codiceRegione = records.get(0).getCodiceRegione();

        if (territorialeAnagrafica == null) {
            ObjectFactory objSismTerAnag = getObjectFactory();

            territorialeAnagrafica = objSismTerAnag.createTerritorialeAnagrafica();
            territorialeAnagrafica.setAnnoRiferimento(annoRif);
            territorialeAnagrafica.setCodiceRegione(codiceRegione);
            territorialeAnagrafica.setPeriodoRiferimento(PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));

            for (RecordDtoSismTerritorialeAnag r : records) {
                if (!r.getTipoOperazioneAnagrafica().equalsIgnoreCase("NM")) {
                    creaAnagraficaXml(r, territorialeAnagrafica, objSismTerAnag);
                }
            }

        }
        return territorialeAnagrafica;
    }

    public ConfigurazioneFlussoSismTerritorialeAnag getConfigurazione() {
        return new ConfigurazioneFlussoSismTerritorialeAnag();
    }

    private ObjectFactory getObjectFactory() {
        return new ObjectFactory();
    }

    public TerritorialeAnagrafica.AziendaSanitariaRiferimento getCurrentAsl(TerritorialeAnagrafica territorialeAnagrafica, RecordDtoSismTerritorialeAnag r) {
        return territorialeAnagrafica.getAziendaSanitariaRiferimento()
                .stream()
                .filter(asl -> r.getCodiceAziendaSanitariaRiferimento().equalsIgnoreCase(asl.getCodiceAziendaSanitariaRiferimento()))
                .findFirst()
                .orElse(null);
    }

    public TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM getCurrentDsm(TerritorialeAnagrafica.AziendaSanitariaRiferimento currentAsl, RecordDtoSismTerritorialeAnag r) {
        return currentAsl.getDSM()
                .stream()
                .filter(dsm -> r.getCodiceDipartimentoSaluteMentale().equalsIgnoreCase(dsm.getCodiceDSM()))
                .findFirst()
                .orElse(null);
    }
}
