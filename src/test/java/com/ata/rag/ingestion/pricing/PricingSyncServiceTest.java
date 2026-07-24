package com.ata.rag.ingestion.pricing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ata.rag.config.RagProperties;
import com.ata.rag.ingestion.pipeline.ProcessedPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingSyncServiceTest {

    @Test
    void normalizesRawTuitionRows() throws Exception {
        RagProperties properties = new RagProperties(
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
                "test-agent");
        PricingSyncService service = new PricingSyncService(properties, new ObjectMapper(), null, null);
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
    }
}
