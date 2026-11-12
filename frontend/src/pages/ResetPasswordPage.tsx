import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "../api/auth";
import { publishAuth } from "../api/authSync";
import { ApiError, getErrorMessage } from "../api/errors";
import { Alert } from "../components/ui/Alert";
import { Button, buttonClasses } from "../components/ui/Button";
import { Field, controlAttrs } from "../components/ui/Field";
import { PasswordInput } from "../components/ui/PasswordInput";
import { AuthCard } from "../features/auth/AuthCard";
import { RateLimitAlert } from "../features/auth/RateLimitAlert";
import { PASSWORD_HINT, resetPasswordSchema } from "../features/auth/schemas";
import { useRateLimit } from "../features/auth/useRateLimit";
import { useSingleSubmit } from "../features/auth/useSingleSubmit";
import { applyFieldErrors } from "../lib/applyFieldErrors";
import { formatCountdown } from "../lib/format";
import { usePageMeta } from "../lib/usePageMeta";
import { useAuthStore } from "../stores/auth";
import { toast } from "../stores/toast";

interface ResetValues {
  newPassword: string;
  confirmPassword: string;
}

function isLinkProblem(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.code === "TOKEN_INVALID" || error.code === "TOKEN_EXPIRED")
  );
}

function LinkExpired() {
  return (
    <AuthCard
      icon="expired"
      titleSize="status"
      align="start"
      title="Ссылка устарела"
      focusTitle
    >
      <p className="leading-[1.6] text-ink-soft">
        Эта ссылка уже использована или срок её действия истёк. Запросите новую
        — это займёт минуту.
      </p>
      <Link
        to="/forgot-password"
        className={buttonClasses({ variant: "dark", size: "md" })}
      >
        Запросить новую ссылку
      </Link>
    </AuthCard>
  );
}

export default function ResetPasswordPage() {
  usePageMeta({
    title: "Новый пароль | KitapRadar",
    description: "Задайте новый пароль для входа в KitapRadar.",
  });
  const [params] = useSearchParams();
  const token = params.get("token")?.trim() || null;
  const navigate = useNavigate();
  const rateLimit = useRateLimit();
  const [linkBroken, setLinkBroken] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ResetValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
  });
  const mutation = useMutation({
    mutationFn: (values: ResetValues) =>
      resetPassword(token ?? "", values.newPassword),
    meta: { silentError: true },
  });

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null);
      try {
        await mutation.mutateAsync(values);
      } catch (error) {
        if (rateLimit.handle(error)) return;
        if (isLinkProblem(error)) {
          setLinkBroken(true);
          return;
        }
        if (applyFieldErrors(error, setError, ["newPassword"])) return;
        setFormError(getErrorMessage(error));
        return;
      }
      // the server ended every session of the account
      if (useAuthStore.getState().status === "authenticated") {
        useAuthStore.getState().clearSession();
        publishAuth({ type: "logout" });
      }
      toast.success("Пароль изменён, войдите");
      navigate("/login", { replace: true });
    }),
  );

  if (!token || linkBroken) return <LinkExpired />;

  return (
    <AuthCard title="Новый пароль">
      <form
        noValidate
        onSubmit={(event) => void submit(event)}
        className="flex flex-col gap-4"
      >
        {formError && <Alert tone="danger">{formError}</Alert>}
        <RateLimitAlert rateLimit={rateLimit} />
        <Field
          label="Новый пароль"
          hint={PASSWORD_HINT}
          error={errors.newPassword?.message}
        >
          {(props) => (
            <PasswordInput
              {...controlAttrs(props)}
              autoComplete="new-password"
              {...register("newPassword")}
            />
          )}
        </Field>
        <Field label="Повторите пароль" error={errors.confirmPassword?.message}>
          {(props) => (
            <PasswordInput
              {...controlAttrs(props)}
              autoComplete="new-password"
              {...register("confirmPassword")}
            />
          )}
        </Field>
        <Button
          type="submit"
          size="form"
          className="w-full"
          loading={mutation.isPending}
          disabled={rateLimit.blocked}
        >
          {rateLimit.blocked
            ? `Сохранить пароль · ${formatCountdown(rateLimit.secondsLeft)}`
            : "Сохранить пароль"}
        </Button>
      </form>
      <p className="text-[13px] text-muted">
        После смены пароля выходим на всех устройствах — войдите заново.
      </p>
    </AuthCard>
  );
}
