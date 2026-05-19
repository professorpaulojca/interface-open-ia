const API_BASE_URL = "http://localhost:8080/api/documentos";
const API_ORIGIN = new URL(API_BASE_URL).origin;

const promptInput = document.getElementById("prompt");
const fileInput = document.getElementById("file");
const generateButton = document.getElementById("generateButton");
const historyButton = document.getElementById("historyButton");
const clearButton = document.getElementById("clearButton");
const statusBox = document.getElementById("status");
const historyBox = document.getElementById("history");
const historyBadge = document.getElementById("historyBadge");
const resultContent = document.getElementById("resultContent");
const dropzone = document.getElementById("dropzone");
const filePreview = document.getElementById("filePreview");
const sessionRequests = document.getElementById("sessionRequests");
const sessionSuccess = document.getElementById("sessionSuccess");
const sessionRate = document.getElementById("sessionRate");

const stats = {
    requests: 0,
    success: 0
};

generateButton.addEventListener("click", generateDocument);
historyButton.addEventListener("click", loadHistory);
clearButton.addEventListener("click", clearHistory);
fileInput.addEventListener("change", updateFilePreview);
promptInput.addEventListener("keydown", (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
        generateDocument();
    }
});

initializeDropzone();
updateSessionStats();
updateFilePreview();

loadHistory();

async function generateDocument() {
    const prompt = promptInput.value.trim();
    const selectedFile = fileInput.files?.[0] || null;

    if (!prompt) {
        setStatus("Digite um prompt antes de gerar o arquivo.", "error");
        return;
    }

    setLoading(true);
    stats.requests += 1;
    updateSessionStats();
    setStatus("Enviando prompt e arquivo (opcional). Aguarde a geração...", "info");

    const formData = new FormData();
    formData.append("prompt", prompt);
    if (selectedFile) {
        formData.append("file", selectedFile);
    }

    try {
        const response = await fetch(`${API_BASE_URL}/gerar`, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response));
        }

        const data = await response.json();
        const downloadUrl = buildDownloadUrl(data.downloadUrl);
        const filename = data.filename || "documento_gerado";
        renderResult(data, selectedFile?.name || null, downloadUrl);
        downloadFile(downloadUrl, filename);
        stats.success += 1;
        updateSessionStats();
        setStatus(`Arquivo gerado com sucesso: ${filename}.`, "success");
        await loadHistory();
    } catch (error) {
        updateSessionStats();
        setStatus(`Erro: ${formatError(error)}`, "error");
    } finally {
        setLoading(false);
    }
}

async function loadHistory() {
    try {
        const response = await fetch(`${API_BASE_URL}/historico`);
        if (!response.ok) {
            throw new Error(await readErrorMessage(response));
        }
        const data = await response.json();
        renderHistory(data.historico || "Histórico vazio.");
    } catch (error) {
        historyBox.textContent = `Não foi possível carregar o histórico. ${formatError(error)}`;
        updateHistoryBadge(0);
    }
}

async function clearHistory() {
    setLoading(true);
    setStatus("Limpando histórico...", "info");
    try {
        const response = await fetch(`${API_BASE_URL}/historico`, { method: "DELETE" });
        if (!response.ok) {
            throw new Error(await readErrorMessage(response));
        }
        const data = await response.json();
        promptInput.value = "";
        fileInput.value = "";
        updateFilePreview();
        renderHistory("Histórico vazio.");
        setStatus(data.mensagem || "Histórico apagado.", "success");
    } catch (error) {
        setStatus(`Não foi possível limpar o histórico. ${formatError(error)}`, "error");
    } finally {
        setLoading(false);
    }
}

function downloadFile(url, filename) {
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
}

function buildDownloadUrl(downloadUrl) {
    if (!downloadUrl) {
        throw new Error("Backend não retornou o link de download.");
    }

    if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
        return downloadUrl;
    }

    return `${API_ORIGIN}${downloadUrl}`;
}

function renderHistory(historyText) {
    historyBox.innerHTML = "";
    const lines = historyText.split("\n");
    let renderedLines = 0;

    for (const line of lines) {
        const trimmedLine = line.trim();
        if (trimmedLine) {
            renderedLines += 1;
        }

        if (line.includes("<a ")) {
            appendSafeHistoryHtmlLine(line);
        } else {
            historyBox.append(line);
        }
        historyBox.append("\n");
    }

    updateHistoryBadge(renderedLines);
}

function appendSafeHistoryHtmlLine(line) {
    const template = document.createElement("template");
    template.innerHTML = line.trim();

    for (const node of template.content.childNodes) {
        if (node.nodeType === Node.TEXT_NODE) {
            historyBox.append(node.textContent);
            continue;
        }

        if (node.nodeType === Node.ELEMENT_NODE && node.tagName === "A") {
            const href = node.getAttribute("href") || "";
            const isAbsoluteAllowed = href.startsWith(`${API_ORIGIN}/api/documentos/download/`);
            const isRelativeAllowed = href.startsWith("/api/documentos/download/");

            if (!isAbsoluteAllowed && !isRelativeAllowed) {
                historyBox.append(node.textContent || href);
                continue;
            }

            const link = document.createElement("a");
            link.href = buildDownloadUrl(href);
            link.textContent = node.textContent || href;
            link.target = "_blank";
            link.rel = "noopener noreferrer";
            historyBox.appendChild(link);
        }
    }
}

function setStatus(message, type = "info") {
    statusBox.textContent = message;
    statusBox.classList.remove("status--info", "status--success", "status--error");
    statusBox.classList.add(`status--${type}`);
}

function setLoading(loading) {
    generateButton.disabled = loading;
    historyButton.disabled = loading;
    clearButton.disabled = loading;
    fileInput.disabled = loading;
    if (dropzone) {
        dropzone.classList.toggle("is-disabled", loading);
    }

    if (loading) {
        generateButton.textContent = "Gerando...";
    } else {
        generateButton.textContent = "Gerar arquivo";
    }
}

function renderResult(data, attachmentName, downloadUrl) {
    const safeMessage = escapeHtml(data.mensagem || "Arquivo gerado.");
    const safeFilename = escapeHtml(data.filename || "documento_gerado");
    const safeType = escapeHtml(data.contentType || "desconhecido");
    const safeAttachment = attachmentName ? escapeHtml(attachmentName) : "Sem anexo";
    const safeSize = typeof data.size === "number" ? `${(data.size / 1024).toFixed(1)} KB` : "--";

    resultContent.innerHTML = `
        <p class="result-message">${safeMessage}</p>
        <div class="result-grid">
            <span class="chip"><strong>Arquivo:</strong> ${safeFilename}</span>
            <span class="chip"><strong>Tipo:</strong> ${safeType}</span>
            <span class="chip"><strong>Tamanho:</strong> ${safeSize}</span>
            <span class="chip"><strong>Anexo:</strong> ${safeAttachment}</span>
        </div>
        <a class="download-link" href="${downloadUrl}" target="_blank" rel="noopener noreferrer">Baixar novamente</a>
    `;
}

function updateHistoryBadge(linesCount) {
    if (!historyBadge) {
        return;
    }
    const suffix = linesCount === 1 ? "linha" : "linhas";
    historyBadge.textContent = `${linesCount} ${suffix}`;
}

function updateFilePreview() {
    if (!filePreview) {
        return;
    }

    const selectedFile = fileInput.files?.[0] || null;
    if (!selectedFile) {
        filePreview.innerHTML = "Nenhum arquivo selecionado.";
        return;
    }

    const ext = selectedFile.name.includes(".") ? selectedFile.name.split(".").pop().toLowerCase() : "--";
    const sizeKb = (selectedFile.size / 1024).toFixed(1);
    filePreview.innerHTML = `
        <span class="chip"><strong>Arquivo:</strong> ${escapeHtml(selectedFile.name)}</span>
        <span class="chip"><strong>Tipo:</strong> .${escapeHtml(ext)}</span>
        <span class="chip"><strong>Tamanho:</strong> ${sizeKb} KB</span>
    `;
}

function initializeDropzone() {
    if (!dropzone) {
        return;
    }

    const preventDefaults = (event) => {
        event.preventDefault();
        event.stopPropagation();
    };

    ["dragenter", "dragover", "dragleave", "drop"].forEach((eventName) => {
        dropzone.addEventListener(eventName, preventDefaults);
    });

    ["dragenter", "dragover"].forEach((eventName) => {
        dropzone.addEventListener(eventName, () => {
            if (!generateButton.disabled) {
                dropzone.classList.add("is-active");
            }
        });
    });

    ["dragleave", "drop"].forEach((eventName) => {
        dropzone.addEventListener(eventName, () => {
            dropzone.classList.remove("is-active");
        });
    });

    dropzone.addEventListener("drop", (event) => {
        if (generateButton.disabled) {
            return;
        }

        const files = event.dataTransfer?.files;
        if (!files || files.length === 0) {
            return;
        }

        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(files[0]);
        fileInput.files = dataTransfer.files;
        updateFilePreview();
    });
}

function updateSessionStats() {
    if (!sessionRequests || !sessionSuccess || !sessionRate) {
        return;
    }

    sessionRequests.textContent = String(stats.requests);
    sessionSuccess.textContent = String(stats.success);
    const rate = stats.requests === 0 ? 0 : Math.round((stats.success / stats.requests) * 100);
    sessionRate.textContent = `${rate}%`;
}

function escapeHtml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatError(error) {
    const message = typeof error === "string" ? error : error?.message || "";
    if (message.includes("Failed to fetch") || message.includes("NetworkError")) {
        return "Backend offline ou inacessível em http://localhost:8080.";
    }
    if (message.includes("Maximum upload size exceeded") || message.includes("Request Entity Too Large") || message.includes("413")) {
        return "Arquivo excede o tamanho máximo permitido pelo servidor.";
    }
    return message || "Erro desconhecido.";
}

async function readErrorMessage(response) {
    try {
        const data = await response.json();
        return data.mensagem || data.detalhe || `Erro HTTP ${response.status}`;
    } catch {
        return await response.text();
    }
}
