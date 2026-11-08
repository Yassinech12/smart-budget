# 💶 Smart Budget – Full Stack budget app with AI categorization

Smart Budget is a personal budget app. You import your bank statement as a CSV file, every transaction gets a category automatically, and you follow your spending against monthly budgets on a dashboard.

The back-end is a Spring Boot REST API secured with JWT, the front-end is a React and TypeScript single page app, the data is stored in PostgreSQL, and a Mistral LLM is used for the transactions that the keyword rules don't recognise. Everything runs with Docker Compose, and a GitHub Actions pipeline runs the tests on every push.

Dashboard

Why I built it

I wanted a full stack project with a real back-end: authentication, a relational database, business rules, file upload and automated tests, not only a front-end calling a public API. Categorizing bank transactions is also a good example of a problem where an LLM is useful but should not be the only solution: most labels are easy to recognise with simple rules, and the model is only needed for the rest. That connects this project with the data and AI work I do.

Features
Accounts and security
Sign up and log in with email and password. Passwords are hashed with BCrypt.
After login the API returns a JWT that the front-end sends in the Authorization header. The API is stateless: there is no server session.
Each user only sees and edits their own data. This is checked in every query and covered by an integration test.
Transactions
Add, edit and delete transactions manually. A negative amount is an expense, a positive amount is an income.
Import a bank statement in CSV. The parser accepts ; or , as separator, French dates (02/10/2026) or ISO dates (2026-10-02), French amounts (-1 234,56) or international ones (-1234.56), quoted fields and the Excel BOM. The columns can be called date, label/libellé and amount/montant, in any order.
Lines that can't be read don't stop the import: they are listed in the import report with their line number.
Filter the transactions by month and by category.
Automatic categorization

Every transaction without a category goes through 3 steps:

Keyword rules. The label is normalised (lower case, accents removed) and compared to lists of keywords for each category: supermarkets for food, Navigo or SNCF for transport, Netflix or Free Mobile for subscriptions, and so on. The order of the rules matters, for example "Uber Eats" is checked as a restaurant before "Uber" as transport. A positive amount that matches nothing is counted as income.
LLM. If no rule matches and a Mistral API key is configured, the label is sent to Mistral with a short prompt that lists the allowed categories. The answer is parsed in a tolerant way (extra words or punctuation are ignored).
Default. If there is no API key or the call fails, the transaction goes to "Other". The app keeps working without the LLM.

The source of each category (rule, AI, manual or default) is saved and shown in the transactions table, and the user can change the category in one click. A manual change is then marked as manual.

There are 10 categories: food, housing, transport, health, leisure, shopping, subscriptions, restaurants, income and other.

Budgets and alerts
One monthly limit per category.
The dashboard shows how much of each budget is used, with a warning at 80% and an alert when the budget is exceeded.
Dashboard

For the selected month: total income, total expenses, balance, number of transactions, spending by category (donut chart), income and expenses over the last 6 months (bar chart), and the progress of each budget.

Transactions	Budgets
Transactions	Budgets
Architecture
REST / JSON + JWT
/api
unknown labels
React + TypeScriptVite, Recharts
nginx
Spring Boot APISpring Security, Spring DataJPA
PostgreSQL
Mistral API

In Docker, nginx serves the React build and forwards /api to the Spring Boot container, so the front-end and the API share the same origin. In development, Vite does the same with its proxy.

Back-end

Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, Bean Validation, jjwt, springdoc-openapi.

The code is organised by feature rather than by technical layer:

backend/src/main/java/com/yassinecharit/budget/
├── user/             User entity, registration and login
├── security/         JWT service, authentication filter, current user
├── transaction/      Transaction entity, CSV parser, service, controller
├── categorization/   keyword rules, Mistral client, categorization service
├── budget/           Budget entity, service, controller
├── dashboard/        monthly figures, budget alerts, 6-month trend
├── config/           Spring Security, CORS, Swagger, demo data
└── common/           errors returned as JSON

Data model:

Table	Main columns
users	email (unique), password hash, full name, creation date
transactions	user, date, label, amount, category, categorization source (indexed on user and date)
budgets	user, category, monthly limit (one budget per user and category)

Main endpoints (full documentation in Swagger at /swagger-ui.html):

Method	Endpoint	Description
POST	/api/auth/register	create an account, returns a JWT
POST	/api/auth/login	log in, returns a JWT
GET	/api/auth/me	current user
GET	/api/transactions?month=2026-10&category=SHOPPING	transactions of a month, optional category filter
POST	/api/transactions	add a transaction (categorized automatically if no category is given)
PUT	/api/transactions/{id}	edit a transaction
DELETE	/api/transactions/{id}	delete a transaction
POST	/api/transactions/import	import a CSV file (multipart)
GET	/api/budgets	list the budgets
PUT	/api/budgets	create or update the budget of a category
DELETE	/api/budgets/{category}	delete a budget
GET	/api/dashboard?month=2026-10	dashboard data for a month

Errors (validation, not found, wrong password, invalid CSV) are returned as JSON in the RFC 7807 "problem details" format, with the right HTTP status.

Front-end

React 18, TypeScript, Vite, React Router and Recharts. A typed API client handles the JWT and logs the user out if the token expires. The layout works on mobile and follows the system dark mode. Each category keeps the same colour on every chart.

Technical choices
Stateless JWT authentication. No server session, the token is sent in a header, so CSRF protection is not needed. The JWT filter only runs inside the Spring Security chain.
Rules before the LLM. The LLM is called only when the rules fail, which keeps the import fast, costs almost nothing and makes the app usable offline.
BigDecimal for money. Never double, to avoid rounding errors.
One user, one scope. Every repository query filters on the logged-in user instead of trusting an ID sent by the front-end.
Demo profile. With the demo Spring profile, the app creates a demo account with 3 months of transactions and 4 budgets, so anyone can try it in one command.
Run it
With Docker
bash
git clone https://github.com/Yassinech12/smart-budget.git
cd smart-budget
cp .env.example .env     # optional, see the configuration below
docker compose up --build
App: http://localhost:3000, demo account demo@smartbudget.dev / demo1234
API documentation: http://localhost:8080/swagger-ui.html
Without Docker

You need Java 21, Maven, Node 20 and a local PostgreSQL with a smartbudget database.

bash
# API
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo

# Front-end, on http://localhost:5173
cd frontend
npm install
npm run dev

A sample bank statement is available in sample-data/releve_bancaire_exemple.csv.

Configuration
Variable	Default	Description
DB_URL, DB_USER, DB_PASSWORD	local smartbudget database	PostgreSQL connection
JWT_SECRET	random key at each start	Base64 key of at least 256 bits (openssl rand -base64 48)
JWT_EXPIRATION_HOURS	24	token lifetime
MISTRAL_API_KEY	empty	enables the LLM categorization
MISTRAL_MODEL	mistral-small-latest	Mistral model
CORS_ORIGINS	http://localhost:5173,http://localhost:3000	allowed front-end origins
Tests
bash
cd backend && mvn verify    # unit tests and API tests on an in-memory H2 database
cd frontend && npm test     # Vitest

What is tested:

categorization rules, accents, income detection, LLM call and fallback
CSV parsing: French and ISO formats, quotes, invalid lines, missing columns
budget alert levels
the API end to end with MockMvc: registration, login, wrong password, validation errors, a protected route without token, automatic categorization, CSV import, budget and dashboard figures, and data isolation between two users
front-end formatting helpers (amounts, dates, months)

The GitHub Actions pipeline has 3 jobs: back-end build and tests, front-end tests and build, and the Docker images build.

What I would add next
detection of recurring transactions (rent, subscriptions) and a forecast of the end of the month
Flyway migrations instead of letting Hibernate update the schema
refresh tokens and an account settings page
an online demo

Yassine CHARIT · LinkedIn · GitHub

---

👤 **Yassine CHARIT** · [LinkedIn](https://www.linkedin.com/in/yassine-charit/) · [GitHub](https://github.com/Yassinech12)
