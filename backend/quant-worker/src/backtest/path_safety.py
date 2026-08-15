from __future__ import annotations

import os
from pathlib import Path


class UnsafeBacktestPathError(ValueError):
    pass


def require_confined_directory(root: Path, path: Path) -> Path:
    root_absolute = root.absolute()
    path_absolute = path.absolute()
    try:
        path_absolute.relative_to(root_absolute)
    except ValueError as exc:
        raise UnsafeBacktestPathError(f"path is outside root: {path}") from exc
    if root.is_symlink() or path.is_symlink():
        raise UnsafeBacktestPathError(f"symlinked directory is not allowed: {path}")
    root_resolved = root.resolve()
    path_resolved = path.resolve()
    if not path_resolved.is_relative_to(root_resolved):
        raise UnsafeBacktestPathError(f"resolved directory is outside root: {path}")
    if not path_resolved.is_dir():
        raise UnsafeBacktestPathError(f"required directory is missing: {path}")
    return path_resolved


def require_confined_regular_file(root: Path, path: Path) -> Path:
    root_resolved = require_confined_directory(root, root)
    path_absolute = path.absolute()
    try:
        path_absolute.relative_to(root.absolute())
    except ValueError as exc:
        raise UnsafeBacktestPathError(f"file is outside root: {path}") from exc
    if path.is_symlink():
        raise UnsafeBacktestPathError(f"symlinked file is not allowed: {path}")
    path_resolved = path.resolve()
    if not path_resolved.is_relative_to(root_resolved):
        raise UnsafeBacktestPathError(f"resolved file is outside root: {path}")
    if not path_resolved.is_file():
        raise UnsafeBacktestPathError(f"regular file is required: {path}")
    return path_resolved


def create_confined_directory(root: Path, path: Path) -> Path:
    require_confined_directory(root, root)
    try:
        relative = path.absolute().relative_to(root.absolute())
    except ValueError as exc:
        raise UnsafeBacktestPathError(f"directory is outside root: {path}") from exc
    current = root
    for part in relative.parts:
        current = current / part
        current.mkdir(exist_ok=True)
        require_confined_directory(root, current)
    return path


def write_new_confined_bytes(root: Path, path: Path, payload: bytes) -> Path:
    """Create one regular file without following a destination symlink."""

    parent = require_confined_directory(root, path.parent)
    try:
        path.absolute().relative_to(root.absolute())
    except ValueError as exc:
        raise UnsafeBacktestPathError(f"file is outside root: {path}") from exc
    if path.exists() or path.is_symlink():
        raise UnsafeBacktestPathError(
            f"confined output file already exists or is a symlink: {path}"
        )

    directory_flags = os.O_RDONLY | getattr(os, "O_DIRECTORY", 0)
    file_flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
    directory_fd = os.open(parent, directory_flags)
    try:
        try:
            file_fd = os.open(
                path.name,
                file_flags,
                0o600,
                dir_fd=directory_fd,
            )
        except FileExistsError as exc:
            raise UnsafeBacktestPathError(
                f"confined output file already exists or is a symlink: {path}"
            ) from exc
        with os.fdopen(file_fd, "wb") as output:
            output.write(payload)
    finally:
        os.close(directory_fd)
    return path


def write_new_confined_text(root: Path, path: Path, content: str) -> Path:
    return write_new_confined_bytes(root, path, content.encode("utf-8"))


def copy_to_new_confined_file(root: Path, source: Path, destination: Path) -> Path:
    if source.is_symlink() or not source.is_file():
        raise UnsafeBacktestPathError(
            f"confined copy source must be a regular non-symlink file: {source}"
        )
    return write_new_confined_bytes(root, destination, source.read_bytes())


def require_confined_tree(root: Path) -> None:
    require_confined_directory(root, root)
    for path in root.rglob("*"):
        if path.is_symlink():
            raise UnsafeBacktestPathError(f"symlink is not allowed in run tree: {path}")
        resolved = path.resolve()
        if not resolved.is_relative_to(root.resolve()):
            raise UnsafeBacktestPathError(f"path resolves outside run tree: {path}")
        if not path.is_dir() and not path.is_file():
            raise UnsafeBacktestPathError(
                f"non-regular run artifact is not allowed: {path}"
            )
