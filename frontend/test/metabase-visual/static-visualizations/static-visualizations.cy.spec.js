import { restore, setupSMTP, cypressWaitAll } from "__support__/e2e/cypress";
import { USERS } from "__support__/e2e/cypress_data";

const { admin } = USERS;

const visualizationTypes = ["line", "area", "bar", "combo"];

const createQuestionAndAddToDashboard = (query, dashboardId) => {
  return cy.createQuestion(query).then(response => {
    cy.request("POST", `/api/dashboard/${dashboardId}/cards`, {
      cardId: response.body.id,
    });
  });
};

const createOneDimensionTwoMetricsQuestion = (display, dashboardId) => {
  return createQuestionAndAddToDashboard(
    {
      name: `${display} one dimension two metrics`,
      query: {
        "source-table": 2,
        aggregation: [["count"], ["avg", ["field", 15, null]]],
        breakout: [["field", 12, { "temporal-unit": "month" }]],
      },
      display: display,
      database: 1,
    },
    dashboardId,
  );
};

const createOneMetricTwoDimensionsQuestion = (display, dashboardId) => {
  return createQuestionAndAddToDashboard(
    {
      name: `${display} one metric two dimensions`,
      query: {
        "source-table": 2,
        aggregation: [["count"]],
        breakout: [
          ["field", 12, { "temporal-unit": "month" }],
          ["field", 4, { "source-field": 13 }],
        ],
      },
      display: display,
      database: 1,
    },
    dashboardId,
  );
};

describe("static visualizations", () => {
  beforeEach(() => {
    restore();
    cy.signInAsAdmin();
    setupSMTP();
  });

  visualizationTypes.map(type => {
    it(`${type} chart`, () => {
      cy.createDashboard()
        .then(({ body: { id: dashboardId } }) => {
          return cypressWaitAll(
            createOneMetricTwoDimensionsQuestion(type, dashboardId),
            createOneDimensionTwoMetricsQuestion(type, dashboardId),
          ).then(() => {
            cy.visit(`/dashboard/${dashboardId}`);
          });
        })
        .then(() => {
          cy.icon("share").click();
          cy.findByText("Dashboard subscriptions").click();

          cy.findByText("Email it").click();
          cy.findByPlaceholderText("Enter user names or email addresses")
            .click()
            .type(`${admin.first_name} ${admin.last_name}{enter}`)
            .blur();

          cy.button("Send email now").click();
          cy.button("Email sent");

          cy.window().then(win => (win.location.href = "http://localhost:80"));
          cy.findByText("Test Dashboard").click();

          cy.findByText("Display");
          cy.percySnapshot();
        });
    });
  });
});
