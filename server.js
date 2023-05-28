const express = require('express');
const app = express();
// This is your test secret API key.
const stripe = require("stripe")('sk_test_51NBLIbJ1TgaWBK1fqtBQ8Q8rKI1pFFbgdplLiEbrKJ7xxfLBM2UCFSBbYfEiKT9RUjSIyi6UY6Cwlyo3fEJda740006hIgM0fb')

app.use(express.static("public"));
// app.use(express.static("."));
app.use(express.json());

const calculateOrderAmount = (items) => {
  // Replace this constant with a calculation of the order's amount
  // Calculate the order total on the server to prevent
  // people from directly manipulating the amount on the client
  return 1400;
};

// PaymentIntent tracks customer's payment lifecycle
// app.post("/create-payment-intent", async (req, res) => {
//   const { items } = req.body;

//   // Create a PaymentIntent with the order amount and currency
//   const paymentIntent = await stripe.paymentIntents.create({
//     amount: calculateOrderAmount(items),
//     currency: "pln",
//     automatic_payment_methods: {
//       enabled: true,
//     },
//   });

//   res.send({ // returning unique key to let the client access important fields
//     clientSecret: paymentIntent.client_secret,
//   });
// });


// This example sets up an endpoint using the Express framework.
// Watch this video to get started: https://youtu.be/rPR2aJ6XnAc.

app.post('/payment-sheet', async (req, res) => {
  console.log(req.body.price);
  const amountString = req.body.price.slice(0, -1)
  console.log(amountString);

  const paymentAmount = parseInt(amountString) * 100

  // Use an existing Customer ID if this is a returning customer.
  const customer = await stripe.customers.create();
  const ephemeralKey = await stripe.ephemeralKeys.create(
    {customer: customer.id},
    {apiVersion: '2022-11-15'}
  );
  const paymentIntent = await stripe.paymentIntents.create({
    amount: paymentAmount, // 10.99 usd
    currency: 'usd',
    customer: customer.id,
    automatic_payment_methods: {
      enabled: true,
    },
  });

  res.json({
    paymentIntent: paymentIntent.client_secret,
    ephemeralKey: ephemeralKey.secret,
    customer: customer.id,
    publishableKey: 'pk_test_51NBLIbJ1TgaWBK1fl72yC7FhY3gdSSEfl9f0Ui0sHHltto7kxMnXaEUrWodU42KlvRfUdJXsgNPiro0I4MxH4xWv00iYBg2lLf'
  });
});

app.get('/', (req, res) => {
    res.status(200).json({
        message: "Elooo"
    });
})

app.listen(4245, () => console.log("Node server listening on port 4245!"));