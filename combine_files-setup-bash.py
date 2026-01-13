import os
import subprocess

# Назви вихідних файлів
script_filename = "setup.bash"
prompt_filename = "ai_prompt.txt"

# Текст для шапки з розділенням Ролі та Таску
user_prompt_text = """
Act as Senior C++ Developer.

TASK: Implement avro seriazization to kafka topic. Clients will be java and python.

don't change schemas/trade.avsc

use only official binaries and libs for Avro

Return fully corrected, complete setup.bash that recreates everything properly, including all original files plus the consumer services refactor.

CRITICAL RULES (MUST FOLLOW):
- DO NOT delete, rename, move, or simplify any existing file or folder
- DO NOT recreate project from scratch
- DO NOT replace docker-compose.yml — only modify if strictly required
- EVERY file from the original project MUST still exist
- setup.bash MUST recreate the FULL project
- This is a refactor-in-place, not a redesign

Allowed:
- Add new files
- Modify code inside existing files

Environment:
- Windows (Git Bash / WSL)
- setup.bash for scaffolding only
- Use only: cat << 'EOF' > filename

If ANY file from original project is missing → answer is invalid.
Below is the current structure and content of my project for your reference:
"""

exclude_list = {
    'ai_prompt.txt',
    'create_context.py',
    'setup.bash',
    'result_content.txt',
    '.git',
    '.terraform',
    'terraform.tfstate',
    'terraform.tfstate.backup',
    '.terraform.lock.hcl',
    'combine_files-old.py',
    '__pycache__',
    'update.cmd',
    '.idea',
    '.vscode',
    '.ivy2',
    '.gitignore',
    '.pytest_cache',
    'venv',
    'check_data.py',
    'list-warehouse-bucket.cmd',
    'run.bat',
    'run_tests.sh',
    'run_trading_bot.bat',
    'run_trading_bot.sh',
    'kafka_data',
    'localstack_data',
    'orderbook_data_binance',
    'orderbook_data_poloniex',
    'binance-orderbooks-jsonl',
    'data',
    'Dockerfile-kafka-investigation',
    '.gradle',
    'check-all.cmd',
    'gradlew',
    'gradlew.bat',
    'lombok.config',
    'run-snyk-checks.cmd',
    'run.cmd',
    'settings.gradle',
    'sonar-run-check.cmd',
    'sonar-start.cmd',
    'gradle.yml',
    'checkstyle-suppressions.xml',
    'checkstyle.xml',
    'pmd.xml',
    'excludeFilter.xml',
    'gradle-wrapper.properties',
    'file.out',
    'logback-test.xml',
    'nginx.conf',
    'build',
    'build-with-stacktrace.cmd',
    'build.gradle',
    'gradle.properties',
    'exception',
    'AppExceptionHandler.java',
    'AppThreadFactory.java',
    'DownloadException.java',
    'application.yml',
    os.path.basename(__file__)
}

def generate_context():
    files_added = 0
    bash_content = []

    # 1. Формуємо вміст setup.bash у пам'яті
    bash_content.append("#!/bin/bash\n")

    # Рекурсивний обхід
    for root, dirs, files in os.walk('.'):
        # Фільтрація папок
        dirs[:] = [d for d in dirs if d not in exclude_list]

        for filename in files:
            if filename in exclude_list:
                continue

            # Перевірка розширень
            if filename.endswith(('.pyc', '.exe', '.dll')):
                continue

            file_path = os.path.join(root, filename)
            relative_path = os.path.relpath(file_path, '.').replace('\\', '/')
            directory = os.path.dirname(relative_path)

            try:
                with open(file_path, "r", encoding="utf-8") as infile:
                    content = infile.read()

                # Команда створення папки
                if directory and directory != ".":
                    bash_content.append(f"mkdir -p {directory}\n")

                # Команда запису файлу
                bash_content.append(f"cat << 'EOF' > {relative_path}\n")
                bash_content.append(content)
                if not content.endswith('\n'):
                    bash_content.append("\n")
                bash_content.append("EOF\n\n")

                print(f"Додано до контексту: {relative_path}")
                files_added += 1

            except (UnicodeDecodeError, PermissionError):
                continue
            except Exception as e:
                print(f"Помилка з {relative_path}: {e}")

    full_bash_text = "".join(bash_content)

    # 2. Зберігаємо файли
    with open(script_filename, "w", encoding="utf-8", newline='\n') as f_bash:
        f_bash.write(full_bash_text)

    with open(prompt_filename, "w", encoding="utf-8", newline='\n') as f_prompt:
        f_prompt.write(user_prompt_text)
        f_prompt.write("\n" + "#" * 80 + "\n")
        f_prompt.write(f"### ATTACHED setup.bash CONTENT:\n")
        f_prompt.write("#" * 80 + "\n\n")
        f_prompt.write(full_bash_text)

    # 3. Викликаємо unix2dos для перетворення форматів
    print(f"\n{'-'*10} Конвертація в DOS формат {'-'*10}")
    for target in [script_filename, prompt_filename]:
        try:
            # check=True викине помилку, якщо unix2dos не встановлено
            subprocess.run(['unix2dos', target], check=True, capture_output=True)
            print(f"OK: {target} конвертовано.")
        except (subprocess.CalledProcessError, FileNotFoundError):
            print(f"Увага: Не вдалося викликати unix2dos для {target}. Перевірте, чи встановлена утиліта.")

    print(f"\n{'-'*40}")
    print(f"Успіх!")
    print(f"- Скрипт: {script_filename}")
    print(f"- Промпт: {prompt_filename}")
    print(f"Всього файлів упаковано: {files_added}")

if __name__ == "__main__":
    generate_context()