import React from 'react';

interface FormSectionProps {
  title: string;
  children: React.ReactNode;
}

export const FormSection: React.FC<FormSectionProps> = ({ title, children }) => (
  <div className="form-section">
    <h3 className="form-section-title">{title}</h3>
    {children}
  </div>
);
