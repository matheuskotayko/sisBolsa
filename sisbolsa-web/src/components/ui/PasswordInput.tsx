import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

interface PasswordInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  required?: boolean;
}

export const PasswordInput: React.FC<PasswordInputProps> = ({
  label,
  required,
  id,
  className = '',
  ...props
}) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="form-group">
      {label && (
        <label htmlFor={id}>
          {label} {required && <span className="asterisco">*</span>}
        </label>
      )}
      <div className="password-field-wrapper">
        <input
          {...props}
          id={id}
          type={showPassword ? 'text' : 'password'}
          className={className}
        />
        <button
          type="button"
          className="password-toggle-btn"
          onClick={() => setShowPassword(!showPassword)}
          aria-label={showPassword ? 'Ocultar senha' : 'Exibir senha'}
        >
          {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
    </div>
  );
};
