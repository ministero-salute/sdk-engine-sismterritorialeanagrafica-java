/* SPDX-License-Identifier: BSD-3-Clause */

package it.mds.sdk.flusso.sism.territoriale.anag.tracciato;

import it.mds.sdk.flusso.sism.territoriale.anag.parser.regole.RecordDtoSismTerritorialeAnag;
import it.mds.sdk.flusso.sism.territoriale.anag.parser.regole.conf.ConfigurazioneFlussoSismTerritorialeAnag;
import it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.ObjectFactory;
import it.mds.sdk.flusso.sism.territoriale.anag.tracciato.bean.output.anagrafica.TerritorialeAnagrafica;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.dtos.CampiInputBean;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@MockitoSettings(strictness = Strictness.LENIENT)
class TracciatoSplitterImplTest {

    @InjectMocks
    @Spy
    private TracciatoSplitterImpl tracciatoSplitter;
    private ConfigurazioneFlussoSismTerritorialeAnag configurazione = Mockito.mock(ConfigurazioneFlussoSismTerritorialeAnag.class);
    private ObjectFactory objectFactory = Mockito.mock(ObjectFactory.class);
    private TerritorialeAnagrafica territorialeAnagrafica = Mockito.mock(TerritorialeAnagrafica.class);
    private TerritorialeAnagrafica.AziendaSanitariaRiferimento asl = Mockito.mock(TerritorialeAnagrafica.AziendaSanitariaRiferimento.class);
    private ConfigurazioneFlussoSismTerritorialeAnag.XmlOutput xmlOutput = Mockito.mock(ConfigurazioneFlussoSismTerritorialeAnag.XmlOutput.class);
    private MockedStatic<GestoreFileFactory> gestore;
    private GestoreFile gestoreFile = Mockito.mock(GestoreFile.class);
    private TerritorialeAnagrafica.AziendaSanitariaRiferimento aziendaSanitariaRiferimento = Mockito.mock(TerritorialeAnagrafica.AziendaSanitariaRiferimento.class);
    private List<TerritorialeAnagrafica.AziendaSanitariaRiferimento> aziendaSanitariaRiferimentoList = new ArrayList<>();
    private TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM dsm = Mockito.mock(TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM.class);
    private List<TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM> listDsm = new ArrayList<>();
    private TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM.Assistito assistito = Mockito.mock(TerritorialeAnagrafica.AziendaSanitariaRiferimento.DSM.Assistito.class);
    private RecordDtoSismTerritorialeAnag r = new RecordDtoSismTerritorialeAnag();
    List<RecordDtoSismTerritorialeAnag> records = new ArrayList<>();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        gestore = mockStatic(GestoreFileFactory.class);
        initMockedRecord(r);
        records.add(r);
    }

    @Test
    void dividiTracciatoTest() throws JAXBException, IOException, SAXException {

        when(tracciatoSplitter.getConfigurazione()).thenReturn(configurazione);
        when(objectFactory.createTerritorialeAnagrafica()).thenReturn(territorialeAnagrafica);
        when(territorialeAnagrafica.getAziendaSanitariaRiferimento()).thenReturn(List.of(asl));
        when(configurazione.getXmlOutput()).thenReturn(xmlOutput);
        when(xmlOutput.getPercorso()).thenReturn("percorso");
        gestore.when(() -> GestoreFileFactory.getGestoreFile("XML")).thenReturn(gestoreFile);
        doNothing().when(gestoreFile).scriviDto(any(), any(), any());

        Assertions.assertEquals(
                List.of(Path.of("percorsoSDK_TER_ANT_S1_100.xml")),
                this.tracciatoSplitter.dividiTracciato(records, "100")
        );

    }

    @Test
    void dividiTracciatoTestOk2() throws JAXBException, IOException, SAXException {
        when(tracciatoSplitter.getConfigurazione()).thenReturn(configurazione);
        when(objectFactory.createTerritorialeAnagrafica()).thenReturn(territorialeAnagrafica);
        when(territorialeAnagrafica.getAziendaSanitariaRiferimento()).thenReturn(List.of(asl));

        when(configurazione.getXmlOutput()).thenReturn(xmlOutput);
        when(xmlOutput.getPercorso()).thenReturn("percorso");
        gestore.when(() -> GestoreFileFactory.getGestoreFile("XML")).thenReturn(gestoreFile);
        doNothing().when(gestoreFile).scriviDto(any(), any(), any());

        doReturn(null).when(tracciatoSplitter).getCurrentAsl(any(), any());
        doReturn(null).when(tracciatoSplitter).getCurrentDsm(any(), any());

        Assertions.assertEquals(
                List.of(Path.of("percorsoSDK_TER_ANT_S1_100.xml")),
                this.tracciatoSplitter.dividiTracciato(records, "100")
        );

    }

    @Test
    void getCurrentDsmTest() {
        var list = List.of(dsm);
        when(asl.getDSM()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentDsm(asl, r);
    }

    @Test
    void getCurrentAslTest() {
        var list = List.of(asl);

        when(territorialeAnagrafica.getAziendaSanitariaRiferimento()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentAsl(territorialeAnagrafica, r);
    }

    @Test
    void creaPrestazioniSanitarieTest() {
        var list = List.of(asl);
        var c = tracciatoSplitter.creaTerritorialeAnagrafica(records, null);
    }


    @AfterEach
    void closeMocks() {
        gestore.close();
    }

    private void initMockedRecord(RecordDtoSismTerritorialeAnag r) {
        CampiInputBean campiInputBean = new CampiInputBean();
        campiInputBean.setPeriodoRiferimentoInput("Q1");
        campiInputBean.setAnnoRiferimentoInput("2022");
        r.setTipoOperazioneAnagrafica("C");
        r.setAnnoRiferimento("2022");
        r.setCodiceRegione("080");
        r.setPeriodoRiferimento("S1");
        r.setCodiceDipartimentoSaluteMentale("cdsm");
        r.setCodiceAziendaSanitariaRiferimento("casr");
        r.setIdRecord("ic");
        r.setTipologiaCodiceIdentificativoAssistito(1);
        r.setValiditaCodiceIdentificativoAssistito(1);
        records.add(r);
    }
}
