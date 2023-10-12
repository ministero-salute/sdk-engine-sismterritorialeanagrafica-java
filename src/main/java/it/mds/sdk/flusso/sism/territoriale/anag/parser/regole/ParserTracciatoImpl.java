package it.mds.sdk.flusso.sism.territoriale.anag.parser.regole;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import it.mds.sdk.flusso.sism.territoriale.anag.parser.regole.conf.ConfigurazioneFlussoSismTerritorialeAnag;
import it.mds.sdk.libreriaregole.dtos.RecordDtoGenerico;
import it.mds.sdk.libreriaregole.parser.ParserTracciato;
import it.mds.sdk.rest.exception.ParseCSVException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * La classe implementa l'interfaccia ParserTracciato e il suo metodo parseTracciato(File tracciato)
 */
@Slf4j
@Component("parserTracciatoSismTerrAnag")
public class ParserTracciatoImpl implements ParserTracciato {

    /**
     * Il metodo converte un File.csv in una List<RecordDtoGenerico> usando come separatore "~"
     *
     * @param tracciato, File.csv di input
     * @return una lista di RecordDtoDir
     */

    private final ConfigurazioneFlussoSismTerritorialeAnag conf = new ConfigurazioneFlussoSismTerritorialeAnag();

    @Override
    public List<RecordDtoGenerico> parseTracciato(File tracciato) {
        try (FileReader fileReader = new FileReader(tracciato)) {
            char separatore = conf.getSeparatore().getSeparatore().charAt(0);
            List<RecordDtoGenerico> dirList = new CsvToBeanBuilder<RecordDtoGenerico>(fileReader)
                    .withType(RecordDtoSismTerritorialeAnag.class)
                    .withSeparator(separatore)
                    .withSkipLines(1)   //Salta la prima riga del file CSV
                    .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                    .build()
                    .parse();
            fileReader.close();

            return dirList;

        } catch (FileNotFoundException e) {
            log.debug(e.getMessage(), e);
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        } catch (Exception ex) {
            log.debug(ex.getMessage(), ex);
            throw new ParseCSVException(ex.getMessage());
        }

        return Collections.emptyList();
    }

    public List<RecordDtoGenerico> parseTracciatoBlocco(File tracciato, int inizio, int fine) {
        StopWatch stopWatch = new StopWatch();
        log.info("Inizio lettura file {} da riga {} a riga {}", tracciato.getName(), inizio, fine);
        stopWatch.start();
        try (Reader reader = Files.newBufferedReader(tracciato.toPath())) {
            char separatore = conf.getSeparatore().getSeparatore().charAt(0);
            List<RecordDtoGenerico> res = new ArrayList<>();
            Iterator<RecordDtoSismTerritorialeAnag> it = new CsvToBeanBuilder<RecordDtoSismTerritorialeAnag>(reader)
                    .withType(RecordDtoSismTerritorialeAnag.class)
                    .withSeparator(separatore)
                    .withSkipLines(inizio + 1)   //Salta la prima riga del file CSV
                    .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                    .build()
                    .iterator();
            int count = inizio;
            int totaleRecordElaborati = 0;
            while (it.hasNext() && count < fine) {
                count++;
                RecordDtoGenerico recordGen = it.next();
                res.add(recordGen);
                totaleRecordElaborati++;
            }
            stopWatch.stop();
            log.info("Fine lettura file {} da riga {} a riga {} con {} record in {} ms", tracciato.getName(), inizio,
                    fine, totaleRecordElaborati, stopWatch.getLastTaskTimeMillis());

            return res;
        } catch (IOException e) {
            throw new ParseCSVException(e.getMessage());
        } catch (Exception ex) {
            log.debug(ex.getMessage(), ex);
            throw new ParseCSVException(ex.getMessage());
        }
    }
}