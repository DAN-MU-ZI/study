import React, { useEffect, useState } from "react";
import Modal from "./Modal"; // 모달 컴포넌트 임포트

const WebSocketComponent: React.FC = () => {
  const [currentStep, setCurrentStep] = useState<string>("Waiting for process...");
  const [completedSteps, setCompletedSteps] = useState<string[]>([]);
  const [status, setStatus] = useState<string>("In Progress");
  const [showModal, setShowModal] = useState<boolean>(false);

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8000/ws");

    ws.onmessage = (event) => {
      const message = event.data;

      if (message === "Completed") {
        setStatus("Process Completed!");
        ws.close();
      } else {
        setCompletedSteps((prevSteps) => [...prevSteps, message]);
        setCurrentStep(message);
      }
    };

    ws.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    ws.onclose = () => {
      console.log("WebSocket connection closed");
    };

    return () => {
      ws.close();
    };
  }, []);

  return (
    <div style={{ textAlign: "center", padding: "50px" }}>
      <h1>Process Tracker</h1>
      <button
        onClick={() => setShowModal(true)}
        style={{
          padding: "10px 20px",
          backgroundColor: "#007bff",
          color: "#fff",
          border: "none",
          borderRadius: "5px",
          cursor: "pointer",
        }}
      >
        Show Process
      </button>

      {/* 모달 창 */}
      <Modal
        show={showModal}
        onClose={() => setShowModal(false)}
        steps={completedSteps}
        currentStep={currentStep}
        status={status}
      />
    </div>
  );
};

export default WebSocketComponent;
