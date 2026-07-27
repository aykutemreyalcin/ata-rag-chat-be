package com.ata.rag.ingestion.pricing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ata.rag.config.RagProperties;
import com.ata.rag.ingestion.pipeline.ProcessedPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingSyncServiceTest {

    private final PricingSyncService service = new PricingSyncService(
            new RagProperties(
                    "https://akademiata.pl",
                    "https://example.invalid/exec",
                    "https://akademiata.pl/kalkulator-czesnego/",
                    "",
                    "https://api.openai.com/v1",
                    "gpt-4.1-mini",
                    "text-embedding-3-small",
                    1536,
                    0.55,
                    "",
                    "",
                    0,
                    20,
                    false,
                    "test-agent"),
            new ObjectMapper(),
            null,
            null);

    @Test
    void normalizesPolishRawTuitionRows() throws Exception {
        String json =
                """
                {
                  "RAW": {
                    "pl": {
                      "wwa": {
                        "s": [
                          {
                            "k": "Informatyka",
                            "s": null,
                            "deg": 1,
                            "r10": 1000,
                            "r12": 890,
                            "rekr": 85,
                            "wps": 490,
                            "ps": "https://akademiata.pl/oferta/studia-1-stopnia/informatyka/",
                            "ak": "1_wwa_informatyka"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
        List<ProcessedPage> docs = service.normalize(new ObjectMapper().readTree(json));
        assertTrue(docs.stream().anyMatch(doc -> "pricing".equals(doc.sourceType())));
        assertTrue(docs.stream().anyMatch(doc -> doc.markdown().contains("Informatyka")));
        assertTrue(docs.stream().anyMatch(doc -> doc.markdown().contains("1000")));
        assertTrue(docs.stream()
                .anyMatch(doc ->
                        "https://akademiata.pl/oferta/studia-1-stopnia/informatyka/".equals(doc.url())));
    }

    @Test
    void normalizesEnglishWroclawCybersecurityRow() throws Exception {
        String json =
                """
                {
                  "RAW": {
                    "en": {
                      "wro": [
                        {
                          "k": "Computer Engineering",
                          "s": "Computer networks and cybersecurity",
                          "deg": 1,
                          "eu": {"r": 2600, "s": 1400},
                          "ne": {"r": 3000, "s": 1600},
                          "rekr": 200,
                          "wps": 0,
                          "ps": "https://akademiata.pl/en/offer/bachelor/wroclaw-computer-networks-and-cybersecurity/",
                          "ak": "1_wro_wroclaw-computer-networks-and-cybersecurity"
                        }
                      ]
                    },
                    "pl": {
                      "wro": {
                        "s": [
                          {
                            "k": "Informatyka",
                            "s": "Inżynieria sieci i cyberbezpieczeństwo",
                            "deg": 1,
                            "r10": 840,
                            "r12": 710,
                            "rekr": 85,
                            "wps": 490,
                            "ps": "https://akademiata.pl/oferta/studia-1-stopnia/inzynieria-sieci-i-cyberbezpieczenstwo/",
                            "ak": "1_wro_inzynieria-sieci-i-cyberbezpieczenstwo"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
        List<ProcessedPage> docs = service.normalize(new ObjectMapper().readTree(json));
        assertTrue(docs.stream()
                .anyMatch(doc -> doc.markdown().contains("Computer networks and cybersecurity")
                        && doc.markdown().contains("2600")
                        && doc.markdown().contains("EU / CIS / Ukraine")));
        assertTrue(docs.stream()
                .anyMatch(doc -> doc.markdown().contains("Inżynieria sieci i cyberbezpieczeństwo")
                        && doc.markdown().contains("840")
                        && doc.markdown().contains("PLN")));
        assertTrue(docs.stream()
                .anyMatch(doc -> doc.url()
                        .contains("wroclaw-computer-networks-and-cybersecurity")));
    }
}
