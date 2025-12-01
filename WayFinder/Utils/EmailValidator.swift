//
//  EmailValidator.swift
//  WayFinder
//
//  Created for strict email validation
//

import Foundation

struct EmailValidator {
    /// Validates that an email address is a real, properly formatted email
    /// This function performs strict validation to ensure only real email addresses are accepted
    /// - Parameter email: The email string to validate
    /// - Returns: true if the email is valid, false otherwise
    static func isValid(_ email: String) -> Bool {
        // Trim whitespace first
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Must not be empty
        guard !trimmedEmail.isEmpty else { return false }
        
        // Must contain exactly one @ symbol
        let atSymbolCount = trimmedEmail.filter { $0 == "@" }.count
        guard atSymbolCount == 1 else { return false }
        
        // Split by @ to get local and domain parts
        let parts = trimmedEmail.split(separator: "@")
        guard parts.count == 2 else { return false }
        
        let localPart = String(parts[0])
        let domainPart = String(parts[1])
        
        // Local part validation (before @)
        // Must be 1-64 characters, can contain letters, numbers, dots, underscores, hyphens, plus signs
        // Cannot start or end with a dot
        guard localPart.count >= 1 && localPart.count <= 64 else { return false }
        guard !localPart.hasPrefix(".") && !localPart.hasSuffix(".") else { return false }
        guard !localPart.contains("..") else { return false } // No consecutive dots
        
        let localPartRegex = "^[A-Za-z0-9]([A-Za-z0-9._-]*[A-Za-z0-9])?$|^[A-Za-z0-9]+$"
        guard NSPredicate(format: "SELF MATCHES %@", localPartRegex).evaluate(with: localPart) else { return false }
        
        // Domain part validation (after @)
        // Must contain at least one dot (for TLD)
        guard domainPart.contains(".") else { return false }
        
        // Split domain by dots
        let domainParts = domainPart.split(separator: ".")
        guard domainParts.count >= 2 else { return false } // Must have at least domain and TLD
        
        // TLD (last part) must be 2-63 characters and only letters
        let tld = String(domainParts.last!)
        guard tld.count >= 2 && tld.count <= 63 else { return false }
        guard tld.allSatisfy({ $0.isLetter }) else { return false }
        
        // Domain name validation (all parts except TLD)
        let domainName = domainParts.dropLast().joined(separator: ".")
        guard !domainName.isEmpty else { return false }
        guard domainName.count <= 253 else { return false } // Max domain length
        
        // Each domain part must be 1-63 characters
        for part in domainParts {
            let partString = String(part)
            guard partString.count >= 1 && partString.count <= 63 else { return false }
            // Domain parts can contain letters, numbers, and hyphens, but not start/end with hyphen
            guard !partString.hasPrefix("-") && !partString.hasSuffix("-") else { return false }
            let domainPartRegex = "^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?$"
            guard NSPredicate(format: "SELF MATCHES %@", domainPartRegex).evaluate(with: partString) else { return false }
        }
        
        // Final comprehensive regex check as additional validation
        let emailRegex = "^[A-Za-z0-9]([A-Za-z0-9._-]*[A-Za-z0-9])?@[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,63}$"
        return NSPredicate(format: "SELF MATCHES %@", emailRegex).evaluate(with: trimmedEmail.lowercased())
    }
}

