import React, { useCallback, useMemo, useState } from "react";
import { t } from "ttag";
import Users from "metabase/entities/users";
import Link from "metabase/components/Link";
import AuthLayout from "../AuthLayout/AuthLayout";
import { EmailData, ViewType } from "./types";
import {
  FormLink,
  FormTitle,
  InfoBody,
  InfoIcon,
  InfoIconContainer,
  InfoMessage,
} from "./ForgotPassword.styled";

interface ForgotPasswordProps {
  showScene: boolean;
  canResetPassword: boolean;
  initialEmail?: string;
  onResetPassword: (email: string) => void;
}

const ForgotPassword = ({
  showScene,
  canResetPassword,
  initialEmail,
  onResetPassword,
}: ForgotPasswordProps): JSX.Element => {
  const [view, setView] = useState<ViewType>(
    canResetPassword ? "form" : "disabled",
  );

  const handleSubmit = useCallback(
    async (email: string) => {
      await onResetPassword(email);
      setView("success");
    },
    [onResetPassword],
  );

  return (
    <AuthLayout showScene={showScene}>
      {view === "form" && (
        <ForgotPasswordForm
          initialEmail={initialEmail}
          onSubmit={handleSubmit}
        />
      )}
      {view === "success" && <ForgotPasswordSuccess />}
      {view === "disabled" && <ForgotPasswordDisabled />}
    </AuthLayout>
  );
};

interface ForgotPasswordFormProps {
  initialEmail?: string;
  onSubmit: (email: string) => void;
}

const ForgotPasswordForm = ({
  initialEmail,
  onSubmit,
}: ForgotPasswordFormProps): JSX.Element => {
  const initialValues = useMemo(() => {
    return { email: initialEmail };
  }, [initialEmail]);

  const handleSubmit = useCallback(
    async ({ email }: EmailData) => {
      await onSubmit(email);
    },
    [onSubmit],
  );

  return (
    <div>
      <FormTitle>{t`Forgot password`}</FormTitle>
      <Users.Form
        form={Users.forms.password_forgot}
        submitTitle={t`Send password reset email`}
        initialValues={initialValues}
        onSubmit={handleSubmit}
      />
      <FormLink
        className="Button Button--borderless"
        to={"/auth/login"}
      >{t`Back to sign in`}</FormLink>
    </div>
  );
};

const ForgotPasswordSuccess = (): JSX.Element => {
  return (
    <InfoBody>
      <InfoIconContainer>
        <InfoIcon name="check" />
      </InfoIconContainer>
      <InfoMessage>
        {t`Check your email for instructions on how to reset your password.`}
      </InfoMessage>
      <Link
        className="Button Button--primary"
        to={"/auth/login"}
      >{t`Back to sign in`}</Link>
    </InfoBody>
  );
};

const ForgotPasswordDisabled = (): JSX.Element => {
  return (
    <InfoBody>
      <InfoMessage>
        {t`Please contact an administrator to have them reset your password.`}
      </InfoMessage>
    </InfoBody>
  );
};

export default ForgotPassword;
