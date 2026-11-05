# Project Overview

## Purpose

Designer Costume Studio is the foundation of a boutique virtual try-on platform. Customers will eventually browse designs, provide a photo, explore costume/color combinations, save looks, and contact the boutique.

## Foundation, database, and API scope

This phase establishes the monorepo and independently executable service foundations:

- Next.js web application and Expo mobile application
- Shared Spring Boot API and PostgreSQL database
- Normalized persistence entities, repositories, and Flyway migrations for the initial domain
- Health and read-only category/costume APIs
- JWT authentication, rotating refresh sessions, password recovery, and permission-guarded admin user creation
- A minimal FastAPI service for future model-backed try-on work
- Docker Compose configuration and developer documentation

The only UI is a welcome screen. The backend provides health, authentication, and catalog read APIs; the AI service provides a health endpoint. Costume write workflows, uploads, favorites, inquiries, and AI model integration are explicitly deferred.

## Technology choices

- JavaScript is used for both client applications; TypeScript is not configured.
- The Spring Boot API is the shared boundary for web and mobile clients.
- PostgreSQL is the system-of-record database, accessed through Spring Data JPA with Flyway-managed migrations.
- FastAPI remains a separate service boundary so model/runtime dependencies can evolve independently of the Java API.

## Future direction

Subsequent phases can add costume administration, client navigation, image processing, and an open-source try-on model without coupling those concerns to the persistence foundation.
