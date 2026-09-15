"""
Centralized logging. Import get_logger(__name__) in any module instead of
using print() - gives every log line a timestamp, module name, and level,
and writes to both the console and a log file for later debugging.
"""

import logging
import os
from app.config import LOG_LEVEL, LOG_FILE_PATH

os.makedirs(os.path.dirname(LOG_FILE_PATH), exist_ok=True)

_formatter = logging.Formatter(
    "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

_file_handler = logging.FileHandler(LOG_FILE_PATH)
_file_handler.setFormatter(_formatter)

_console_handler = logging.StreamHandler()
_console_handler.setFormatter(_formatter)


def get_logger(name: str) -> logging.Logger:
    logger = logging.getLogger(name)
    if not logger.handlers:  # avoid duplicate handlers on module reload
        logger.setLevel(LOG_LEVEL)
        logger.addHandler(_file_handler)
        logger.addHandler(_console_handler)
    return logger
