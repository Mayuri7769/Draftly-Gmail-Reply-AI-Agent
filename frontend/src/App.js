import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

const API_BASE_URL = "http://localhost:8080/api";

function App() {
  const [emails, setEmails] = useState([]);
  const [history, setHistory] = useState([]);
  const [historyStatus, setHistoryStatus] = useState("ALL");
  const [selectedEmail, setSelectedEmail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  const showNotice = (type, text) => setNotice({ type, text });

  useEffect(() => {
    fetchUnread();
    fetchHistory("ALL");
  }, []);

  const fetchUnread = async () => {
    setLoading(true);
    try {
      // 1. Try to get emails. 'withCredentials: true' is MANDATORY for cookies
      const response = await axios.get(`${API_BASE_URL}/emails/unread`, {
        withCredentials: true
      });

      setEmails(response.data);
      console.log("Emails synced successfully!");
      showNotice("success", "Inbox synced successfully.");
    } catch (error) {
      // 2. If backend says 401 (Unauthorized), redirect to Google
      if (error.response && error.response.status === 401) {
        window.location.href = "http://localhost:8080/oauth2/authorization/google";
      } else {
        console.error("Sync Error:", error);
        const status = error.response?.status;
        const backendMessage =
          typeof error.response?.data === "string"
            ? error.response.data
            : error.response?.data?.message;
        const details = status
          ? `Request failed with status ${status}.${backendMessage ? ` ${backendMessage}` : ""}`
          : "Backend may be unreachable, blocked by CORS, or still starting.";
        showNotice("error", `Could not sync inbox. ${details}`);
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async (status) => {
    try {
      const url = status === "ALL"
        ? `${API_BASE_URL}/history`
        : `${API_BASE_URL}/history?status=${status}`;
      const response = await axios.get(url, { withCredentials: true });
      setHistory(response.data || []);
    } catch (error) {
      showNotice("error", "Could not load history.");
    }
  };

  const generateDraft = async (tone) => {
    if (!selectedEmail) return;
    try {
      const response = await axios.post(
        `${API_BASE_URL}/ai/generate?id=${selectedEmail.id}&tone=${tone}`,
        {},
        { withCredentials: true }
      );
      setSelectedEmail(response.data); // Update preview with AI text
      // Also update the item in the sidebar list
      setEmails(emails.map(e => e.id === selectedEmail.id ? response.data : e));
      showNotice("success", `Generated ${tone} draft.`);
    } catch (error) {
      showNotice("error", "AI generation failed.");
    }
  };

  const approveAndSend = async () => {
    try {
      const bodyParam = encodeURIComponent(selectedEmail.body || "");
      await axios.post(
        `${API_BASE_URL}/drafts/approve-and-send?id=${selectedEmail.id}&editedBody=${bodyParam}`,
        {},
        { withCredentials: true }
      );
      showNotice("success", "Email sent.");
      setSelectedEmail(null);
      fetchUnread();
      fetchHistory(historyStatus);
    } catch (error) {
      showNotice("error", "Send failed.");
    }
  };

  const rejectDraft = async () => {
    if (!selectedEmail) return;
    try {
      await axios.post(
        `${API_BASE_URL}/drafts/reject?id=${selectedEmail.id}`,
        {},
        { withCredentials: true }
      );
      showNotice("success", "Draft rejected.");
      setSelectedEmail(null);
      fetchUnread();
      fetchHistory(historyStatus);
    } catch (error) {
      showNotice("error", "Reject failed.");
    }
  };

  const handleLogout = async () => {
    try {
      await axios.post(`${API_BASE_URL}/auth/logout`, {}, { withCredentials: true });
      showNotice("success", "Logged out successfully.");
      setTimeout(() => {
        window.location.href = "http://localhost:8080/oauth2/authorization/google";
      }, 700);
    } catch (error) {
      showNotice("error", "Logout failed.");
    }
  };

  return (
    <div className="App">
      <header className="header-bar">
        <h1>Draftly AI Agent</h1>
        <div className="header-actions">
          <button onClick={fetchUnread}>Sync Inbox</button>
          <button className="logout-btn" onClick={handleLogout}>Logout</button>
        </div>
      </header>
      {notice && (
        <div className={`notice ${notice.type}`}>
          <span>{notice.text}</span>
          <button onClick={() => setNotice(null)}>x</button>
        </div>
      )}

      <div className="main-layout">
        {/* Sidebar: Email List */}
        <aside className="email-list">
          {loading ? <p>Loading...</p> : emails.map(email => (
            <div
              key={email.id}
              className={`email-card ${selectedEmail?.id === email.id ? 'active' : ''}`}
              onClick={() => setSelectedEmail(email)}
            >
              <strong>{email.sender}</strong>
              <p style={{margin: '5px 0', fontSize: '0.9rem', color: '#666'}}>{email.subject}</p>
            </div>
          ))}
        </aside>

        {/* Main Content: Preview & Editor */}
        <main className="preview-pane">
          {selectedEmail ? (
            <div className="editor-container">
              <h2>{selectedEmail.subject}</h2>
              <p>From: {selectedEmail.sender}</p>

              <div className="tone-controls">
                <span>Generate Draft:</span>
                <button onClick={() => generateDraft('formal')}>Formal</button>
                <button onClick={() => generateDraft('friendly')}>Friendly</button>
                <button onClick={() => generateDraft('concise')}>Concise</button>
              </div>

              <textarea
                className="draft-editor"
                value={selectedEmail.body || ""}
                placeholder="AI draft will appear here..."
                onChange={(e) => setSelectedEmail({...selectedEmail, body: e.target.value})}
              />

              <div className="action-buttons">
                <button className="approve-btn" onClick={approveAndSend}>Approve & Send</button>
                <button className="reject-btn" onClick={rejectDraft}>Discard</button>
              </div>
            </div>
          ) : (
            <div className="empty-state">Select an email to generate an AI response</div>
          )}
        </main>
      </div>
      <section className="history">
        <div className="history-header">
          <h3>Draft History</h3>
          <select
            value={historyStatus}
            onChange={(e) => {
              const newStatus = e.target.value;
              setHistoryStatus(newStatus);
              fetchHistory(newStatus);
            }}
          >
            <option value="ALL">All</option>
            <option value="DRAFT_GENERATED">Draft Generated</option>
            <option value="EDITED">Edited</option>
            <option value="SENT">Sent</option>
            <option value="REJECTED">Rejected</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
        <table>
          <thead>
            <tr>
              <th>Subject</th>
              <th>Sender</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {history.length === 0 ? (
              <tr><td colSpan="3">No history found.</td></tr>
            ) : history.map(item => (
              <tr key={item.id}>
                <td>{item.subject}</td>
                <td>{item.sender}</td>
                <td>{item.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

export default App;