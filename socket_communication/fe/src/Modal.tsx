import React from "react";

interface ModalProps {
    show: boolean;
    onClose: () => void;
    steps: string[];
    currentStep: string;
    status: string;
}

const Modal: React.FC<ModalProps> = ({ show, onClose, steps, currentStep, status }) => {
    if (!show) return null;

    return (
        <div style={modalStyles.overlay}>
            <div style={modalStyles.modal}>
                <h2>Process Status</h2>
                <div style={{ marginBottom: "20px" }}>
                    {steps.map((step, index) => (
                        <div key={index} style={{ padding: "10px", borderBottom: "1px solid gray" }}>
                            {step}
                        </div>
                    ))}
                </div>
                <p>Current Step: {currentStep}</p>
                <p>Status: {status}</p>
                <button onClick={onClose} style={modalStyles.button}>Close</button>
            </div>
        </div>
    );
};

const modalStyles = {
    overlay: {
        position: "fixed" as const,
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.7)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
    },
    modal: {
        backgroundColor: "#fff",
        padding: "20px",
        borderRadius: "10px",
        maxWidth: "500px",
        width: "100%",
    },
    button: {
        marginTop: "20px",
        padding: "10px 20px",
        backgroundColor: "#007bff",
        color: "#fff",
        border: "none",
        borderRadius: "5px",
        cursor: "pointer",
    },
};

export default Modal;
