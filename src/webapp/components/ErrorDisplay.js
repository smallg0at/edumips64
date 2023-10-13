import React from 'react';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import Chip from '@mui/material/Chip';

const ErrorCount = ({ count }) => {
  if (count == 0) {
    return <React.Fragment />;
  }
  return (
      <Chip icon={<ErrorIcon />} label={count} color='error'/>
  );
};
const WarningCount = ({ count }) => {
  if (count == 0) {
    return <React.Fragment />;
  }
  return (
    <Chip icon={<WarningIcon />} label={count} color='warning'/>
  );
};

const ErrorDisplay = ({ parsingErrors }) => {
  const warningCount = parsingErrors
    ? parsingErrors.filter((e) => e.isWarning).length
    : 0;
  const errorCount = parsingErrors
    ? parsingErrors.filter((e) => !e.isWarning).length
    : 0;

  return (
    <>
      <WarningCount count={warningCount} />
      <ErrorCount count={errorCount} />
    </>
  );
};

export default ErrorDisplay;
