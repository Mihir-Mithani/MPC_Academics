# Contributing to MPC_Academics

Thanks for your interest in contributing! This repository is a collection of academic materials for the **Mobile and Pervasive Computing (MPC)** course — including unit notes, assignments, question banks, and previous year papers. Contributions that improve accuracy, add useful resources, or fix errors are welcome.

## Ways to Contribute

- **Fix errors** in existing notes, assignments, or answer keys (typos, incorrect explanations, outdated content, broken files).
- **Add study material** such as unit notes, chapter summaries, or references aligned with the course syllabus.
- **Contribute to the question bank** with additional practice questions or previous year papers.
- **Improve assignments** by clarifying instructions or adding well-explained sample solutions.
- **Report issues** if you spot a mistake but aren't able to fix it yourself.

## Before You Start

1. Check the [existing issues](https://github.com/Mihir-Mithani/MPC_Academics/issues) and [open pull requests](https://github.com/Mihir-Mithani/MPC_Academics/pulls) to avoid duplicating work.
2. For larger additions (e.g. a new unit's worth of notes), open an issue first to discuss the idea before investing significant time.
3. Small fixes (typos, broken links, formatting) can go straight to a pull request without prior discussion.
4. Please review [SECURITY.md](SECURITY.md) if you're reporting a security-related concern rather than a content issue.

## Getting Started

1. **Fork** the repository.
2. **Clone** your fork:
   ```bash
   git clone https://github.com/<your-username>/MPC_Academics.git
   cd MPC_Academics
   ```
3. **Create a branch** for your change:
   ```bash
   git checkout -b add-unit3-notes
   ```
   Use a descriptive branch name, e.g. `fix-questionbank-typo` or `add-previous-year-paper-2025`.

## Making Changes

- This repository currently uses a flat file structure. When adding new material, use clear, consistent, descriptive file names — for example `Unit_3_Notes.pdf` or `Assignment_3.docx` rather than auto-generated or ambiguous names.
- Prefer PDF for finished/reference material and DOCX only when the file is meant to be editable by others.
- If you're replacing or updating an existing file, keep the same file name where possible so links and references elsewhere don't break; otherwise note the change clearly in your PR description.
- Keep documents well-organized internally: use headings, numbered questions, and consistent formatting.
- Avoid committing system/editor files (e.g. `.DS_Store`) — check `.gitignore` and your local Git settings before pushing.

## Submitting Your Contribution

1. Commit your changes with a clear message:
   ```bash
   git add .
   git commit -m "Add Unit 3 notes on wireless network protocols"
   ```
2. Push to your fork:
   ```bash
   git push origin add-unit3-notes
   ```
3. Open a **pull request** against the `main` branch of this repository.
4. In your PR description, briefly explain:
   - What the change is
   - Why it's useful
   - Which files are affected

## Review Process

- Pull requests will be reviewed for accuracy, relevance to the course, and formatting consistency.
- You may be asked to make small revisions before a PR is merged.
- Please be patient — this is a small, actively maintained academic repository.

## Code of Conduct

By participating in this project, you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md). Please be respectful and constructive in all interactions.

## License and Academic Integrity

- This repository is licensed under the [MIT License](LICENSE). By contributing, you agree that your contributions will be made available under the same license.
- Only submit material you have the right to share (your own notes, properly cited sources, or content that is freely shareable for educational purposes).
- Do not submit content that violates academic integrity policies (e.g. answer keys intended to be restricted, plagiarized material).
- If you're unsure whether something is appropriate to contribute, open an issue to ask before submitting a pull request.

## Questions?

If you have questions or need help getting started, feel free to open an issue and tag it with `question`.

Thank you for helping improve MPC_Academics!
