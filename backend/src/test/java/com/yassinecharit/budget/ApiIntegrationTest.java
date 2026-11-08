package com.yassinecharit.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests of the REST API with an in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String registerAndGetToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.dev";
        String body = """
                {"email": "%s", "password": "password123", "fullName": "Test User"}
                """.formatted(email);
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return "Bearer " + node.get("token").asText();
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerThenLogin() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@test.dev";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "password123", "fullName": "Login User"}
                        """.formatted(email)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "password123"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "wrong-password"}
                        """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "not-an-email", "password": "short", "fullName": ""}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsAndAutoCategorizesATransaction() throws Exception {
        String token = registerAndGetToken();
        mvc.perform(post("/api/transactions").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"date": "%s", "label": "CB CARREFOUR MARKET", "amount": -42.50}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("ALIMENTATION"))
                .andExpect(jsonPath("$.categorizedBy").value("RULE"));
    }

    @Test
    void importsCsvAndBuildsTheDashboardWithBudgetAlerts() throws Exception {
        String token = registerAndGetToken();
        YearMonth month = YearMonth.now();
        String csv = "date;label;amount\n"
                + month.atDay(1) + ";VIREMENT RECU SALAIRE;2000,00\n"
                + month.atDay(2) + ";CB CARREFOUR;-120,00\n"
                + month.atDay(3) + ";CB LIDL;-100,00\n"
                + month.atDay(4) + ";CB LIBRAIRIE INCONNUE;-20,00\n";
        MockMultipartFile file = new MockMultipartFile("file", "releve.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/transactions/import").file(file).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(4))
                .andExpect(jsonPath("$.byRules").value(3))
                .andExpect(jsonPath("$.uncategorized").value(1));

        mvc.perform(put("/api/budgets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "ALIMENTATION", "monthlyLimit": 200}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/dashboard").param("month", month.toString()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(2000.0))
                .andExpect(jsonPath("$.expenses").value(240.0))
                .andExpect(jsonPath("$.balance").value(1760.0))
                .andExpect(jsonPath("$.categories[0].category").value("ALIMENTATION"))
                .andExpect(jsonPath("$.categories[0].alert").value("EXCEEDED"))
                .andExpect(jsonPath("$.alerts.length()").value(1))
                .andExpect(jsonPath("$.trend.length()").value(6));
    }

    @Test
    void usersOnlySeeTheirOwnTransactions() throws Exception {
        String alice = registerAndGetToken();
        String bob = registerAndGetToken();
        mvc.perform(post("/api/transactions").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"date": "%s", "label": "CB FNAC", "amount": -89}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/transactions").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
