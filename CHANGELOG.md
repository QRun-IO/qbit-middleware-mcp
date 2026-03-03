# Changelog

All notable changes to QBit Middleware MCP will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial QBit implementation for MCP (Model Context Protocol) middleware
- MCP server exposing QQQ tables as tools (query, count, get, insert, update, delete)
- MCP resources for table list, table schema, and records
- MCP prompts for exploring tables and summarizing data
- Process execution via MCP tools
- Session provider interface with fixed-user and anonymous implementations
- JSON Schema conversion from QQQ field metadata
- Filter input schema builder for query operations
