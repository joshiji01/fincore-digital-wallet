const API_BASE = "";


// =====================================================
// Common helpers
// =====================================================

function getToken() {
    return sessionStorage.getItem("fincore_token");
}

function setToken(token) {
    sessionStorage.setItem("fincore_token", token);
}

function clearToken() {
    sessionStorage.removeItem("fincore_token");
}

function authHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${getToken()}`
    };
}

async function getErrorMessage(response) {

    try {
        const data = await response.json();

        return data.message ||
               data.error ||
               "Request failed";

    } catch {
        return `Request failed (${response.status})`;
    }
}


// =====================================================
// LOGIN
// =====================================================

const loginForm = document.getElementById("loginForm");

if (loginForm) {

    loginForm.addEventListener("submit", async function (event) {

        event.preventDefault();

        const email =
            document.getElementById("loginEmail")
                .value.trim();

        const password =
            document.getElementById("loginPassword")
                .value;

        const message =
            document.getElementById("loginMessage");

        message.textContent = "Logging in...";

        try {

            const response = await fetch(
                `${API_BASE}/api/auth/login`,
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                }
            );

            if (!response.ok) {

                message.textContent =
                    await getErrorMessage(response);

                return;
            }

            const data = await response.json();

            if (!data.token) {

                message.textContent =
                    "Login succeeded but JWT was not returned.";

                return;
            }

            setToken(data.token);

            window.location.href = "/dashboard";

        } catch (error) {

            console.error(error);

            message.textContent =
                "Unable to connect to FinCore Gateway.";
        }
    });
}


// =====================================================
// REGISTER
// =====================================================

const registerForm =
    document.getElementById("registerForm");

if (registerForm) {

    registerForm.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();

            const name =
                document.getElementById("registerName")
                    .value.trim();

            const email =
                document.getElementById("registerEmail")
                    .value.trim();

            const password =
                document.getElementById("registerPassword")
                    .value;

            const message =
                document.getElementById("registerMessage");

            message.textContent =
                "Creating account...";

            try {

                const response = await fetch(
                    `${API_BASE}/api/auth/register`,
                    {
                        method: "POST",

                        headers: {
                            "Content-Type": "application/json"
                        },

                        body: JSON.stringify({
                            name: name,
                            email: email,
                            password: password
                        })
                    }
                );

                if (!response.ok) {

                    message.textContent =
                        await getErrorMessage(response);

                    return;
                }

                message.textContent =
                    "Account created successfully. Redirecting...";

                setTimeout(() => {

                    window.location.href = "/login";

                }, 1000);

            } catch (error) {

                console.error(error);

                message.textContent =
                    "Unable to connect to FinCore Gateway.";
            }
        }
    );
}


// =====================================================
// DASHBOARD
// =====================================================

const dashboard =
    document.querySelector(".dashboard");

let currentWalletId = null;

if (dashboard) {

    if (!getToken()) {

        window.location.href = "/login";

    } else {

        loadDashboard();
    }
}


async function loadDashboard() {

    try {

        // ---------------------------------------------
        // Get logged-in user
        // ---------------------------------------------

        const userResponse =
            await fetch(
                `${API_BASE}/api/users/me`,
                {
                    method: "GET",
                    headers: authHeaders()
                }
            );

        if (userResponse.status === 401) {

            clearToken();

            window.location.href = "/login";

            return;
        }

        if (!userResponse.ok) {

            throw new Error(
                await getErrorMessage(userResponse)
            );
        }

        const user =
            await userResponse.json();


        document.getElementById("userName")
            .textContent = user.name;

        document.getElementById("welcomeName")
            .textContent = user.name;


        // ---------------------------------------------
        // Get wallet
        // ---------------------------------------------

        const walletResponse =
            await fetch(
                `${API_BASE}/api/wallets/user/${user.id}`,
                {
                    method: "GET",
                    headers: authHeaders()
                }
            );

        if (!walletResponse.ok) {

            throw new Error(
                await getErrorMessage(walletResponse)
            );
        }

        const wallet =
            await walletResponse.json();

        currentWalletId = wallet.id;

        document.getElementById("walletId")
            .textContent = wallet.id;

        updateBalance(wallet.balance);


        // ---------------------------------------------
        // Load transaction history
        // ---------------------------------------------

        await loadTransactions();

    } catch (error) {

        console.error(error);

        document.getElementById("walletBalance")
            .textContent = "Unable to load";
    }
}


// =====================================================
// BALANCE
// =====================================================

function updateBalance(balance) {

    const value =
        Number(balance || 0);

    document.getElementById("walletBalance")
        .textContent =
        `₹${value.toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;
}


// =====================================================
// DEPOSIT
// =====================================================

const depositForm =
    document.getElementById("depositForm");

if (depositForm) {

    depositForm.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();

            const amount =
                Number(
                    document.getElementById(
                        "depositAmount"
                    ).value
                );

            const message =
                document.getElementById(
                    "depositMessage"
                );

            if (!currentWalletId) {

                message.textContent =
                    "Wallet is not loaded.";

                return;
            }

            if (amount <= 0) {

                message.textContent =
                    "Amount must be greater than zero.";

                return;
            }

            message.textContent =
                "Processing deposit...";

            try {

                const response =
                    await fetch(
                        `${API_BASE}/api/wallets/${currentWalletId}/deposit?amount=${amount}`,
                        {
                            method: "POST",
                            headers: authHeaders()
                        }
                    );

                if (!response.ok) {

                    message.textContent =
                        await getErrorMessage(response);

                    return;
                }

                const wallet =
                    await response.json();

                updateBalance(wallet.balance);

                message.textContent =
                    "Deposit successful.";

                document.getElementById(
                    "depositAmount"
                ).value = "";

                await loadTransactions();

            } catch (error) {

                console.error(error);

                message.textContent =
                    "Deposit failed.";
            }
        }
    );
}


// =====================================================
// TRANSFER
// =====================================================

const transferForm =
    document.getElementById("transferForm");

if (transferForm) {

    transferForm.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();

            const receiverWalletId =
                document.getElementById(
                    "receiverWalletId"
                ).value.trim();

            const amount =
                Number(
                    document.getElementById(
                        "transferAmount"
                    ).value
                );

            const message =
                document.getElementById(
                    "transferMessage"
                );

            if (!currentWalletId) {

                message.textContent =
                    "Wallet is not loaded.";

                return;
            }

            if (!receiverWalletId) {

                message.textContent =
                    "Receiver wallet ID is required.";

                return;
            }

            if (amount <= 0) {

                message.textContent =
                    "Amount must be greater than zero.";

                return;
            }

            message.textContent =
                "Processing transfer...";

            try {

                const idempotencyKey =
                    crypto.randomUUID();

                const response =
                    await fetch(
                        `${API_BASE}/api/wallets/transfer`,
                        {
                            method: "POST",

                            headers: {
                                ...authHeaders(),

                                "Idempotency-Key":
                                    idempotencyKey
                            },

                            body: JSON.stringify({
                                senderWalletId:
                                    currentWalletId,

                                receiverWalletId:
                                    receiverWalletId,

                                amount: amount
                            })
                        }
                    );

                if (!response.ok) {

                    message.textContent =
                        await getErrorMessage(response);

                    return;
                }

                const data =
                    await response.json();

                message.textContent =
                    `Transfer successful. Reference ID: ${data.referenceId}`;

                document.getElementById(
                    "receiverWalletId"
                ).value = "";

                document.getElementById(
                    "transferAmount"
                ).value = "";

                await refreshWallet();

                await loadTransactions();

            } catch (error) {

                console.error(error);

                message.textContent =
                    "Transfer failed.";
            }
        }
    );
}


// =====================================================
// REFRESH WALLET
// =====================================================

async function refreshWallet() {

    if (!currentWalletId) {
        return;
    }

    try {

        const response =
            await fetch(
                `${API_BASE}/api/wallets/${currentWalletId}`,
                {
                    method: "GET",
                    headers: authHeaders()
                }
            );

        if (!response.ok) {
            return;
        }

        const wallet =
            await response.json();

        updateBalance(wallet.balance);

    } catch (error) {

        console.error(error);
    }
}


// =====================================================
// TRANSACTION HISTORY
// =====================================================

async function loadTransactions() {

    const container =
        document.getElementById(
            "transactionsContainer"
        );

    if (!container || !currentWalletId) {
        return;
    }

    container.innerHTML = `
        <div class="loading">
            Loading transactions...
        </div>
    `;

    try {

        const response =
            await fetch(
                `${API_BASE}/api/transactions/wallet/${currentWalletId}?page=0&size=10&sort=createdAt,desc`,
                {
                    method: "GET",
                    headers: authHeaders()
                }
            );

        if (!response.ok) {

            container.innerHTML = `
                <div class="empty">
                    Unable to load transactions.
                </div>
            `;

            return;
        }

        const data =
            await response.json();

        const transactions =
            data.content || [];

        if (transactions.length === 0) {

            container.innerHTML = `
                <div class="empty">
                    No transactions yet.
                </div>
            `;

            return;
        }

        container.innerHTML =
            transactions
                .map(transaction => {

                    const isIncoming =
                        transaction.receiverWalletId ===
                        currentWalletId;

                    const sign =
                        isIncoming ? "+" : "-";

                    const amount =
                        Number(transaction.amount)
                            .toLocaleString(
                                "en-IN",
                                {
                                    minimumFractionDigits: 2,
                                    maximumFractionDigits: 2
                                }
                            );

                    const statusClass =
                        transaction.status === "SUCCESS"
                            ? "success"
                            : "failed";

                    return `
                        <div class="transaction">

                            <div>

                                <div class="transaction-type">
                                    ${transaction.type}
                                </div>

                                <div class="transaction-reference">
                                    ${transaction.referenceId}
                                </div>

                            </div>

                            <div class="transaction-right">

                                <div class="transaction-amount">
                                    ${sign} ₹${amount}
                                </div>

                                <div class="transaction-status ${statusClass}">
                                    ${transaction.status}
                                </div>

                            </div>

                        </div>
                    `;

                })
                .join("");

    } catch (error) {

        console.error(error);

        container.innerHTML = `
            <div class="empty">
                Unable to load transactions.
            </div>
        `;
    }
}


// =====================================================
// REFRESH BUTTON
// =====================================================

const refreshBtn =
    document.getElementById("refreshBtn");

if (refreshBtn) {

    refreshBtn.addEventListener(
        "click",
        async function () {

            await refreshWallet();

            await loadTransactions();
        }
    );
}


// =====================================================
// LOGOUT
// =====================================================

const logoutBtn =
    document.getElementById("logoutBtn");

if (logoutBtn) {

    logoutBtn.addEventListener(
        "click",
        function () {

            clearToken();

            window.location.href = "/login";
        }
    );
}