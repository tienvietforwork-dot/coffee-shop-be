@echo off
rem Double-click friendly wrapper for run-all.ps1 (same arguments, e.g. run-all.cmd core,crm)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all.ps1" %*
